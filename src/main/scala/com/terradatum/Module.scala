package com.terradatum

import com.terradatum.dao.UserDao
import slick.basic.DatabaseConfig
import slick.jdbc.PostgresProfile

/**
  * @author rbellamy@terradatum.com
  */
class Module extends scaldi.Module {
  bind[DatabaseConfig[PostgresProfile]] to DatabaseConfig.forConfig[PostgresProfile]("default")
  bind[UserDao] to new UserDao
}
