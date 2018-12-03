package eu.atbk.photos.sorter.exif

import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.Files
import scala.collection.JavaConverters._
import com.drew.imaging.ImageMetadataReader
import com.drew.metadata.exif.ExifIFD0Directory
import com.drew.metadata.exif.ExifDirectoryBase
import java.text.SimpleDateFormat
import java.util.TimeZone
import java.time.ZoneId

object ESMain {
  val in = Paths.get("in")
  val out = Paths.get("out")

  def main(args: Array[String]) {
    val i = Files.walk(in)

    val seq = try {
      i.iterator().asScala
        .filterNot { p =>
          p.endsWith(".gitignore")
        }
        .filter { p =>
          val l = p.getFileName.toString().toLowerCase()
          l.endsWith(".jpg") || l.endsWith(".jpeg")
        }.map { p =>
          //          println(p)
          val inStr = Files.newInputStream(p)
          val meta = try {
            ImageMetadataReader.readMetadata(inStr)
          } finally {
            inStr.close()
          }

          val date = meta.getDirectoriesOfType(classOf[ExifIFD0Directory]).iterator().asScala
            .find(_.containsTag(ExifDirectoryBase.TAG_DATETIME))
            .map(_.getDate(ExifDirectoryBase.TAG_DATETIME)).get

          //          meta.getDirectories.iterator().asScala.foreach { d =>
          //            println(d.getName)
          //          }
          (date, p)
        }.toVector.sortBy(_._1)
    } finally {
      i.close()
    }

    val df = new SimpleDateFormat("yyyyMMddHHmmssSSS")
    df.setTimeZone(TimeZone.getTimeZone("UTC"))

    val o = Files.list(out)

    try {
      o.iterator().asScala
        .filterNot { p =>
          p.endsWith(".gitignore")
        }.foreach(Files.delete)

    } finally {
      o.close()
    }

    seq.foreach { p =>
      val suffix = p._2.getFileName.toString()
      val name = s"${df.format(p._1)}_$suffix"
      val po = out.resolve(name)
      Files.copy(p._2, po)
    }
  }
}
