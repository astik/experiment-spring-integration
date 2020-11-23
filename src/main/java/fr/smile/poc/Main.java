package fr.smile.poc;

import java.io.File;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.core.GenericSelector;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.Pollers;
import org.springframework.integration.file.FileReadingMessageSource;
import org.springframework.integration.file.FileWritingMessageHandler;
import org.springframework.integration.file.transformer.FileToStringTransformer;
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
    public GenericSelector<File> onlyCSV() {
        return new GenericSelector<File>() {
            @Override
            public boolean accept(File source) {
                return source.getName().endsWith(".csv");
            }
        };
    }

    @Bean
    public FileToStringTransformer fileToStringTransformer() {
        return new FileToStringTransformer();
    }

    @Bean
    public IntegrationFlow fileMover() {
        log.trace("fileMover");
        return IntegrationFlows //
                .from(sourceDirectory(), configurer -> configurer.poller(Pollers.fixedDelay(5000))) //
                .filter(onlyCSV()) //
                .transform(fileToStringTransformer()) //
                .handle(targetDirectory()) //
                .get();
    }
}
