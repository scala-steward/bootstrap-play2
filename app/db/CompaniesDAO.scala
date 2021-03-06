package db

import java.util.UUID

import common.daos.BaseSlickDAO
import common.results.Results.Result
import common.results.errors.Errors.{ DatabaseError, NotFound }
import db.codegen.XPostgresProfile
import javax.inject.{ Inject, Singleton }
import slick.jdbc.JdbcBackend.Database
import play.api.libs.json.Json
import models.api.{ Company => CompanyObject }
import dbdata.Tables
import org.joda.time.DateTime
import models.api.Company.patch

import scala.concurrent.{ ExecutionContext, Future }
import scala.language.implicitConversions

/**
 * An implementation dependent DAO.  This could be implemented by Slick, Cassandra, or a REST API.
 */
trait CompaniesDAO {

  def lookup(id: UUID): Future[Result[CompanyObject]]

  def internal_lookupByEmail(email: String): Future[Option[CompanyObject]]

  def create(CompanyObject: CompanyObject): Future[Result[CompanyObject]]

  def update(CompanyObject: CompanyObject): Future[Result[CompanyObject]]

  def delete(id: UUID): Future[Result[Boolean]]

  def close(): Future[Unit]
}

/**
 * A CompanyObject DAO implemented with Slick, leveraging Slick code gen.
 *
 * Note that you must run "flyway/flywayMigrate" before "compile" here.
 *
 * @param db the slick database that this CompanyObject DAO is using internally, bound through Module.
 * @param ec a CPU bound execution context.  Slick manages blocking JDBC calls with its
 *    own internal thread pool, so Play's default execution context is fine here.
 */
@Singleton
class SlickCompaniesSlickDAO @Inject() (db: Database)(implicit ec: ExecutionContext)
    extends BaseSlickDAO(db)
    with CompaniesDAO {

  // Class Name for identification in Database Errors
  override val currentClassForDatabaseError = "SlickCompaniesDAO"

  override val profile: XPostgresProfile.type = XPostgresProfile
  import profile.api._

  /* - - - Compiled Queries - - - */

  private val queryById = Compiled((id: Rep[UUID]) => Company.filter(_.id === id))

  private val queryByEmail = Compiled((email: Rep[String]) =>
    Company.filter { cs =>
      // email === firebaseUser.any is like calling .includes(email)
      email === cs.firebaseUser.any
    }
  )

  /**
   * Lookup single object
   * @param id
   * @return
   */
  def lookup(id: UUID): Future[Result[CompanyObject]] =
    lookupGeneric[CompanyRow, CompanyObject](
      queryById(id).result.headOption
    )

  /**
   * Lookup Company by Email
   * @param email
   * @return
   */
  def internal_lookupByEmail(email: String): Future[Option[CompanyObject]] = {
    val f: Future[Option[CompanyRow]] =
      db.run(queryByEmail(email).result.headOption)
    f.map {
      case Some(row) =>
        Some(companyRowToCompanyObject(row))
      case None      =>
        None
    }
  }

  /**
   * Patch object
   * @param companyObject
   * @return
   */
  def update(companyObject: CompanyObject): Future[Result[CompanyObject]] =
    updateGeneric[CompanyRow, CompanyObject](
      queryById(companyObject.id.getOrElse(UUID.randomUUID())).result.headOption,
      (toPatch: CompanyObject) =>
        queryById(companyObject.id.getOrElse(UUID.randomUUID())).update(companyObjectToCompanyRow(toPatch)),
      (old: CompanyObject) => patch(companyObject, old)
    )

  /**
   * Delete Object
   * @param id
   * @return
   */
  def delete(id: UUID): Future[Result[Boolean]] =
    deleteGeneric[CompanyRow, CompanyObject](
      queryById(id).result.headOption,
      queryById(id).delete
    )

  /**
   * Create new Object
   * @param companyObject
   * @return
   */
  def create(companyObject: CompanyObject): Future[Result[CompanyObject]] =
    createGeneric[CompanyRow, CompanyObject](
      companyObject,
      queryById(companyObject.id.getOrElse(UUID.randomUUID())).result.headOption,
      (entityToSave: CompanyRow) => (Company returning Company) += entityToSave
    )

  /* - - - Mapper Functions - - - */

  implicit private def companyObjectToCompanyRow(CompanyObject: CompanyObject): CompanyRow =
    CompanyRow(
      id = CompanyObject.id.getOrElse(UUID.randomUUID()),
      firebaseUser = CompanyObject.firebaseUser.getOrElse(List.empty[String]),
      settings = CompanyObject.settings.getOrElse(Json.parse("{}")),
      created = CompanyObject.created.getOrElse(DateTime.now),
      updated = CompanyObject.updated.getOrElse(DateTime.now)
    )

  implicit private def companyRowToCompanyObject(companyRow: CompanyRow): CompanyObject =
    CompanyObject(
      id = Some(companyRow.id),
      firebaseUser = Some(companyRow.firebaseUser),
      settings = Some(companyRow.settings),
      created = Some(companyRow.created),
      updated = Some(companyRow.updated)
    )

}
