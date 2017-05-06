package com.terradatum.dao

import com.terradatum.dao
import scaldi.Injector
import scaldi.Injectable._
import slick.basic.DatabaseConfig
import slick.jdbc.PostgresProfile

import scala.concurrent.Future

class UserDao(implicit inj: Injector) {
  private val dbConfig = inject[DatabaseConfig[PostgresProfile]]

  import com.terradatum.dao.Tables._
  import profile.api._

  private val db = dbConfig.db

  private def string2Opt(string: String) =
    if (string.isEmpty) None
    else Some(string)

  private def opt2String(opt: Option[String]) = opt match {
      case Some(string) => string
      case None => ""
    }

  def get(id: String): Future[Seq[dao.Tables.ProfileRow]] = {
    db run (Profile filter { _.id === id } result)
  }
}