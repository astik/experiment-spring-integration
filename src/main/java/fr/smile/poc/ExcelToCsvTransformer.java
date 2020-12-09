package fr.smile.poc;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.io.FilenameUtils;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.integration.file.FileHeaders;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.transformer.AbstractTransformer;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.MessagingException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ExcelToCsvTransformer extends AbstractTransformer {
	@Override
	protected Object doTransform(Message<?> message) {
		log.trace("doTransform");
		String inputFileName = (String) message.getHeaders().get(FileHeaders.FILENAME);
		String inputFileNameExtension = FilenameUtils.getExtension(inputFileName);
		Workbook workbook;
		switch (inputFileNameExtension) {
		case "xls":
			workbook = getHSSFWorkbook(message);
			break;
		case "xlsx":
			workbook = getXSSFWorkbook(message);
			break;
		default:
			throw new IllegalArgumentException(String.format(
					"Unsupported filename extension '%s'. " + "The only supported payload types are xls, xlsx",
					inputFileNameExtension));
		}
		if (workbook == null) {
			throw new MessagingException(message, "The XlsxToCsv tranformation could'nt be done");
		}
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			writeWorkbookAsCsvToOutputStream(workbook, out);
		} catch (IOException e) {
			throw new MessagingException(message, "The XlsxToCsv tranformation could'nt be done", e);
		}
		String newFileName = FilenameUtils.getBaseName(inputFileName) + ".csv";
		return MessageBuilder //
				.withPayload(out.toByteArray())//
				.copyHeaders(message.getHeaders())//
				.setHeader(FileHeaders.FILENAME, newFileName)//
				.build();

	}

	private XSSFWorkbook getXSSFWorkbook(Message<?> message) {
		log.trace("getXSSFWorkbook");
		try {
			XSSFWorkbook workBook;
			final Object payload = message.getPayload();
			if (payload instanceof File) {
				final File filePayload = (File) payload;
				workBook = new XSSFWorkbook(filePayload);
			} else if (payload instanceof byte[]) {
				InputStream inputStream = new ByteArrayInputStream((byte[]) payload);
				workBook = new XSSFWorkbook(inputStream);
			} else {
				throw new IllegalArgumentException(String.format(
						"Unsupported payload type '%s'. "
								+ "The only supported payload types are java.io.File, byte[] and java.io.InputStream",
						payload.getClass().getSimpleName()));
			}
			return workBook;
		} catch (Exception e) {
			throw new MessageHandlingException(message, "Failed to apply Workbook to CSV transformation.", e);
		}
	}

	private HSSFWorkbook getHSSFWorkbook(Message<?> message) {
		log.trace("getHSSFWorkbook");
		try {
			HSSFWorkbook workBook;
			final Object payload = message.getPayload();
			if (payload instanceof File) {
				final File filePayload = (File) payload;
				try (InputStream is = new FileInputStream(filePayload)) {
					workBook = new HSSFWorkbook(is);
				}
			} else if (payload instanceof byte[]) {
				InputStream inputStream = new ByteArrayInputStream((byte[]) payload);
				workBook = new HSSFWorkbook(inputStream);
			} else {
				throw new IllegalArgumentException(String.format(
						"Unsupported payload type '%s'. "
								+ "The only supported payload types are java.io.File, byte[] and java.io.InputStream",
						payload.getClass().getSimpleName()));
			}
			return workBook;
		} catch (Exception e) {
			throw new MessageHandlingException(message, "Failed to apply Workbook to CSV transformation.", e);
		}
	}

	private void writeWorkbookAsCsvToOutputStream(Workbook workbook, OutputStream out) throws IOException {
		log.trace("writeWorkbookAsCsvToOutputStream");
		try (CSVPrinter csvPrinter = new CSVPrinter(new OutputStreamWriter(out), CSVFormat.DEFAULT)) {
			Sheet sheet = workbook.getSheetAt(0); // Sheet #0 in this example
			for (Row row : sheet) {
				if (row != null && row.getLastCellNum() > 0) {
					for (Cell cell : row) {
						switch (cell.getCellType()) {
						case BOOLEAN:
							csvPrinter.print(cell.getBooleanCellValue());
							break;
						case NUMERIC:
							csvPrinter.print(cell.getNumericCellValue());
							break;
						case STRING:
							csvPrinter.print(cell.getStringCellValue());
							break;
						case BLANK:
							csvPrinter.print("");
							break;
						default:
							csvPrinter.print(cell);
						}
					}
					csvPrinter.println();
				}
			}
		}
	}
}
