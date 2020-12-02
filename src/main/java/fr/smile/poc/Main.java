package fr.smile.poc;

import java.io.File;
import java.nio.charset.StandardCharsets;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.annotation.Transformer;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.Pollers;
import org.springframework.integration.file.FileReadingMessageSource;
import org.springframework.integration.file.FileWritingMessageHandler;
import org.springframework.integration.file.transformer.FileToByteArrayTransformer;
import org.springframework.integration.handler.GenericHandler;
import org.springframework.integration.zip.splitter.UnZipResultSplitter;
import org.springframework.integration.zip.transformer.UnZipTransformer;
import org.springframework.integration.zip.transformer.ZipResultType;
import org.springframework.messaging.MessageHandler;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootApplication
public class Main {
    public static void main(String[] args) {
        log.trace("main");
        SpringApplication.run(Main.class, args);
    }

    public static final String INPUT_DIR = "data/source";
    public static final String OUTPUT_DIR = "data/target";

    @Bean
    public MessageSource<File> sourceDirectory() {
        log.trace("sourceDirectory");
        FileReadingMessageSource messageSource = new FileReadingMessageSource();
        messageSource.setDirectory(new File(INPUT_DIR));
        return messageSource;
    }

    @Bean
    public MessageHandler targetDirectory() {
        log.trace("targetDirectory");
        FileWritingMessageHandler handler = new FileWritingMessageHandler(new File(OUTPUT_DIR));
        handler.setExpectReply(false);
        return handler;
    }

    @Bean
    public FileToByteArrayTransformer fileToByteArrayTransformer() {
        return new FileToByteArrayTransformer();
    }

    @Bean
    public UnZipTransformer unZipTransformer() {
        UnZipTransformer unZipTransformer = new UnZipTransformer();
        unZipTransformer.setZipResultType(ZipResultType.BYTE_ARRAY);
        return unZipTransformer;
    }

    @Bean
    public UnZipResultSplitter unZipSplitter() {
        UnZipResultSplitter unZipResultSplitter = new UnZipResultSplitter();
        return unZipResultSplitter;
    }

    @Bean
    DirectChannel inputByteArrayChannel() {
        log.trace("inputByteArrayChannel");
        return new DirectChannel();
    }

    @Bean
    DirectChannel csvChannel() {
        log.trace("csvChannel");
        return new DirectChannel();
    }

    @Bean
    DirectChannel zipChannel() {
        log.trace("zipChannel");
        return new DirectChannel();
    }

    @Bean
    public IntegrationFlow loadFile() {
        log.trace("loadFile");
        return IntegrationFlows //
                .from(sourceDirectory(), configurer -> configurer.poller(Pollers.fixedDelay(5000))) //
                .transform(fileToByteArrayTransformer()) //
                .channel(inputByteArrayChannel()) //
                .get();
    }

    @Bean
    public IntegrationFlow dispatchByteArray() {
        log.trace("dispatchByteArray");
        return IntegrationFlows //
                .from("inputByteArrayChannel") //
                .route("T(org.apache.commons.io.FilenameUtils).getExtension(headers['file_name'])",
                        routerConfigurer -> {
                            routerConfigurer.resolutionRequired(false);
                            routerConfigurer.channelMapping("csv", csvChannel());
                            routerConfigurer.channelMapping("zip", zipChannel());
                            routerConfigurer.defaultOutputChannel(IntegrationContextUtils.ERROR_CHANNEL_BEAN_NAME);
                        }) //
                .get();
    }

    @Bean
    public IntegrationFlow processCsv() {
        log.trace("processCsv");
        return IntegrationFlows //
                .from(csvChannel()) //
                .channel("out") //
                .get();
    }

    @Bean
    public IntegrationFlow processZip() {
        log.trace("processZip");
        return IntegrationFlows //
                .from(zipChannel()) //
                .transform(unZipTransformer()) //
                .split(unZipSplitter()) //
                .filter("!headers['file_name'].startsWith('.')") //
                .channel(inputByteArrayChannel()) //
                .get();
    }

    @Bean
    @Transformer(inputChannel = "out", outputChannel = IntegrationContextUtils.ERROR_CHANNEL_BEAN_NAME)
    public GenericHandler<byte[]> logHandler() {
        return (payload, headers) -> {
            System.out.println("------------> logHandler");
            System.out.println("---> headers " + headers);
            System.out.println("---> payload " + new String(payload, StandardCharsets.UTF_8));
            return payload;
        };
    }
}
