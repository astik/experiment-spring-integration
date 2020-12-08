package fr.smile.poc;

import java.io.File;
import java.nio.charset.StandardCharsets;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.Pollers;
import org.springframework.integration.file.FileHeaders;
import org.springframework.integration.file.FileReadingMessageSource;
import org.springframework.integration.file.FileWritingMessageHandler;
import org.springframework.integration.file.transformer.FileToByteArrayTransformer;
import org.springframework.integration.handler.GenericHandler;
import org.springframework.integration.zip.splitter.UnZipResultSplitter;
import org.springframework.integration.zip.transformer.UnZipTransformer;
import org.springframework.integration.zip.transformer.ZipResultType;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class Config {

	public static final String INPUT_DIR = "data/source";
	public static final String OUTPUT_DIR = "data/target";

	@Bean
	DirectChannel inputByteArrayChannel() {
		log.trace("inputByteArrayChannel");
		return new DirectChannel();
	}

	@Bean
	public IntegrationFlow loadFile() {
		log.trace("loadFile");
		// source
		FileReadingMessageSource sourceDirectory = new FileReadingMessageSource();
		sourceDirectory.setDirectory(new File(INPUT_DIR));
		// transformer
		FileToByteArrayTransformer fileToByteArrayTransformer = new FileToByteArrayTransformer();
		// flow
		return IntegrationFlows //
				.from(sourceDirectory, configurer -> configurer.poller(Pollers.fixedDelay(5000))) //
				.transform(fileToByteArrayTransformer) //
				.channel(inputByteArrayChannel()) //
				.get();
	}

	@Bean
	public IntegrationFlow dispatchByteArray() {
		log.trace("dispatchByteArray");
		return IntegrationFlows //
				.from(inputByteArrayChannel()) //
				.route("T(org.apache.commons.io.FilenameUtils).getExtension(headers['" + FileHeaders.FILENAME + "'])",
						routerConfigurer -> {
							routerConfigurer.resolutionRequired(false);
							routerConfigurer.subFlowMapping("csv", csvSubFlow());
							routerConfigurer.subFlowMapping("zip", zipSubFlow());
							routerConfigurer.subFlowMapping("xls", excelSubFlow());
							routerConfigurer.subFlowMapping("xlsx", excelSubFlow());
							routerConfigurer.defaultOutputChannel(IntegrationContextUtils.ERROR_CHANNEL_BEAN_NAME);
						}) //
				.get();
	}

	private IntegrationFlow csvSubFlow() {
		log.trace("csvSubFlow");
		// log handler
		GenericHandler<byte[]> logHandler = (payload, headers) -> {
			System.out.println("------------> logHandler");
			System.out.println("---> headers " + headers);
			System.out.println("---> payload " + new String(payload, StandardCharsets.UTF_8));
			return payload;
		};
		// output
		FileWritingMessageHandler targetDirectoryHandler = new FileWritingMessageHandler(new File(OUTPUT_DIR));
		targetDirectoryHandler.setExpectReply(false);
		// flow
		return mapping -> {
			mapping //
					.handle(logHandler) //
					.handle(targetDirectoryHandler);
		};
	}

	private IntegrationFlow zipSubFlow() {
		log.trace("zipSubFlow");
		// transformer
		UnZipTransformer unZipTransformer = new UnZipTransformer();
		unZipTransformer.setZipResultType(ZipResultType.BYTE_ARRAY);
		// splitter
		UnZipResultSplitter unZipSplitter = new UnZipResultSplitter();
		// flow
		return mapping -> {
			mapping //
					.transform(unZipTransformer) //
					.split(unZipSplitter) //
					.filter("!headers['" + FileHeaders.FILENAME + "'].startsWith('.')") //
					.channel(inputByteArrayChannel());
		};
	}

	private IntegrationFlow excelSubFlow() {
		log.trace("excelSubFlow");
		// transformer
		ExcelToCsvTransformer excelToCsvTransformer = new ExcelToCsvTransformer();
		// flow
		return mapping -> {
			mapping //
					.transform(excelToCsvTransformer) //
					.channel(inputByteArrayChannel());
		};
	}
}
