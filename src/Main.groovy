import org.codehaus.groovy.runtime.ResourceGroovyMethods

import java.nio.charset.Charset
import java.nio.charset.UnsupportedCharsetException
import java.time.LocalDateTime

/**
 * input file format
 * ※カラム数も何もかもがあっていないファイル
 * HEADER,id1/id2,id3,col1,col2,col3,col4,col5,col6,col,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,
 * A,B,C,,,
 *
 * output file format
 * HEADER,id1,id3,col1,col2,col3,col4,col5,col6,col,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,
 * 固定の値,A,C,固定の値,,,,固定の値,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,
 */
final int HEADER_ROW = 0
final def FIX_VAL_HEADER_FIRST_COL = "D"
final def COL_ID1 = 0
final def COL_ID3 = 2
final def FILE_SEPARATOR = System.properties['file.separator']
final def LINE_SEPARATOR = System.properties['line.separator']
final
def TEMPLATE = "},001,,,,2,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,"
final Charset CHARSET = Charset.defaultCharset()

def path = args[0]
def inputCharset = args.length > 1 && parseCharset(args[1]) != null ? parseCharset(args[1]) : CHARSET
def outputCharset = args.length > 2 && parseCharset(args[2]) != null ? parseCharset(args[2]) : CHARSET
def inputFile = new File(path)
def processLine = 0
def fileIndex = 1
println "start ${LocalDateTime.now()}"
def reader = ResourceGroovyMethods.newReader(inputFile, inputCharset.name())
def header = null
reader.eachLine(HEADER_ROW) { it ->
    def outputByte = 0L
    def outputFileName = "conv${fileIndex++}_${inputFile.name}"
    def outputFile = new File("$inputFile.parent$FILE_SEPARATOR$outputFileName")
    outputFile.withWriter(outputCharset.name()) { writer ->
        if (header == null)
            header = it
        reader.find { // break出来るeachみたいな扱い方
            if (outputByte == 0L) {
                writer << header
                writer << LINE_SEPARATOR
                outputByte += header.getBytes(inputCharset).length
                return false
            }
            def tokenize = it.tokenize(",")
            if (tokenize.every { it == null || it == "NULL" }) return false
            def tmp = "$FIX_VAL_HEADER_FIRST_COL,${tokenize.get(COL_ID1)},{${tokenize.get(COL_ID3)}${TEMPLATE}"
            writer << tmp
            writer << LINE_SEPARATOR
            outputByte += tmp.getBytes(inputCharset).length
            processLine++
            if (processLine % 100 == 0)
                println processLine
            if (outputByte > 9437184L)  // 9MBを超えたら別ファイル
                return true
        }
    }
}

println "end ${LocalDateTime.now()}"


private Charset parseCharset(String charsetString) {
    try {
        return Charset.forName(charsetString)
    } catch (UnsupportedCharsetException e) {
        println "invalid inputCharset. Do you want to \"shiftjis\"? trying \"Windows-31j\""
        e.printStackTrace()
        return null
    }
}
