package net.aotter.resource

import io.vertx.ext.web.RoutingContext
import io.vertx.kotlin.coroutines.await
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter
import org.apache.commons.csv.QuoteMode
import javax.ws.rs.core.HttpHeaders
import javax.ws.rs.core.MediaType

abstract class BaseCSVResource {

    /**
     * generate csv file via streaming (chunked response)
     *
     * @param rc [RoutingContext]
     * @param fileName
     * @param header List of header row column contents
     * @param block function to call for printing csv with two functions as params - printRow: call when printing one row of data, flush: call when printing one batch of data
     */
    suspend fun streamCsv(
        rc: RoutingContext,
        fileName: String,
        header: List<String>,
        block: suspend (printBatch: suspend (List<List<Any?>>) -> Unit) -> Unit
    ): String {

        // prepare chunked response of file download
        val resp = with(rc.response()) {
            putHeader(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN)
            putHeader(
                "Content-Disposition",
                "attachment; filename=$fileName.csv; filename*=utf-8''${
                    java.net.URLEncoder.encode(
                        fileName,
                        "UTF-8"
                    )
                }.csv"
            )
            isChunked = true
            write("\ufeff") // BOM
            this
        }

        val sb = StringBuilder()

        //建立csv檔案
        val format = CSVFormat.EXCEL.builder()
            .setQuoteMode(QuoteMode.ALL)
            .setAutoFlush(true)
            .setHeader(*header.toTypedArray())
            .build()

        //將內容print 到csv中
        CSVPrinter(sb, format).use { printer ->

            val printBatch: suspend (List<List<Any?>>) -> Unit = { lines ->
                // print each line of data with default placeholder "N/A" if null
                lines.forEach { line ->
                    line.map { it ?: "N/A" }.let { printer.printRecord(it) }
                }
                // write to response chunk
                resp.write(sb.toString()).await()
                // clear the builder for next batch of data
                sb.clear()
            }

            // invoke the block and provide printRow function to be called by user
            block(printBatch)
        }

        // let resteasy jackson end the response properly
        return ""
    }

}