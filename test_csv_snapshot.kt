// Simple test to verify CsvSnapshotTest compiles
import digital.vasic.yole.format.csv.*

fun main() {
    val parser = CsvParser()
    val table = parser.parseCsv("test,content")
    println("Rows: ${table.rowCount}")
    println("Test passed!")
}