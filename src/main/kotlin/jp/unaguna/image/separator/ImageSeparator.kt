package jp.unaguna.image.separator

import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import kotlinx.cli.vararg
import java.awt.image.BufferedImage
import java.nio.file.Path
import javax.imageio.ImageIO
import kotlin.io.path.Path
import kotlin.io.path.extension
import kotlin.io.path.nameWithoutExtension

class ImageSeparator {
    companion object {
        @JvmStatic
        fun main(args_: Array<String>) {
            val args = Args(args_)

            for(inputPath in args.inputPaths) {
                separateOneImage(inputPath, args.colNum, args.rowNum, args.reverseCol)
            }
        }

        private class Args(args: Array<String>) {
            private val parser = ArgParser(ImageSeparator::class.qualifiedName ?: "")

            val colNum by parser.option(ArgType.Int, shortName = "c", fullName = "column").default(1)

            val rowNum by parser.option(ArgType.Int, shortName = "r", fullName = "row").default(1)

            val reverseCol by parser.option(ArgType.Boolean, fullName = "reverse-col").default(false)

            val inputPathStr by parser.argument(ArgType.String, fullName = "input_path").vararg()

            val inputPaths by lazy { inputPathStr.map{ Path(it) } }

            init {
                parser.parse(args)
            }
        }

        private fun separateOneImage(inputPath: Path, colNum: Int, rowNum: Int, reverseCol: Boolean) {
            // 画像をロード
            val inputImage = ImageIO.read(inputPath.toFile())

            // 入力画像を分割
            val outputImageMap = separate(inputImage, colNum, rowNum, reverseCol=reverseCol)

            // 分割した画像をファイル出力
            val writer = ImageIO.getImageWritersBySuffix(inputPath.extension).next()
            outputImageMap.forEach { (x, y), newImg ->
                val outputName = "${inputPath.nameWithoutExtension}-${y}-${x}.${inputPath.extension}"
                val outputPath = inputPath.parent.resolve(outputName)

                ImageIO.createImageOutputStream(outputPath.toFile()).use {
                    writer.output = it
                    writer.write(newImg)
                }
            }
        }
    }
}

/**
 * 画像を分割する。
 *
 * @param inputImage 分割する画像
 * @param colNum 横方向の分割数
 * @param rowNum 縦方向の分割数
 * @param reverseCol true の場合、入力画像のより右側から作った分割画像に小さいインデックスを割り当てる。
 * @return 座標に各分割画像 (BufferedImage) を割り当てたマップ。キーは `Pair(x, y)` (ただし `0 <= x < colNum`, `0 <= y < rowNum`)。
 */
fun separate(inputImage: BufferedImage, colNum: Int, rowNum: Int, reverseCol: Boolean = false): SeparatedPartsMap {
    val result: MutableMap<Pair<Int, Int>, BufferedImage> = mutableMapOf()

    val xList = (0..colNum).map { inputImage.width * it / colNum  }
    val yList = (0..rowNum).map { inputImage.height * it / rowNum  }

    for(x in 0 until colNum) {
        for (y in 0 until rowNum) {
            val newImage = inputImage.getSubimage(xList[x], yList[y], xList[x+1]-xList[x], yList[y+1]-yList[y])
            val index = when(reverseCol) {
                false -> Pair(x, y)
                true -> Pair(colNum-1-x, y)
            }
            result[index] = newImage
        }
    }

    return SeparatedPartsMap(result)
}

class SeparatedPartsMap internal constructor(base: Map<Pair<Int, Int>, BufferedImage>): Map<Pair<Int, Int>, BufferedImage> by base {
    /**
     * 画像を取得する。
     *
     * `get(Pair(x,y))` と動作は同じ。
     */
    fun get(x: Int, y: Int) = get(Pair(x,y))
}
