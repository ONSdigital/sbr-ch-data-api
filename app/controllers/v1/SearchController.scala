package controllers.v1

import io.swagger.annotations._
import play.api.mvc.{ Action, AnyContent }
import utils.Utilities._

import javax.inject.Inject

import com.typesafe.config.Config
import play.api.Logger
import models._
import services.CHData

import scala.util.{ Failure, Success }

/**
 * Created by coolit on 18/07/2017.
 */
@Api("Search")
class SearchController @Inject() (chData: CHData, val config: Config) extends ControllerUtils {

  @ApiOperation(
    value = "JSON of the matching company",
    notes = "The company is matched only on id",
    responseContainer = "JSONObject",
    code = 200,
    httpMethod = "GET"
  )
  @ApiResponses(Array(
    new ApiResponse(code = 200, responseContainer = "JSONObject", message = "Success -> Record found for id."),
    new ApiResponse(code = 400, responseContainer = "JSONObject", message = "Client Side Error -> Required parameter was not found."),
    new ApiResponse(code = 404, responseContainer = "JSONObject", message = "Client Side Error -> Id not found."),
    new ApiResponse(code = 500, responseContainer = "JSONObject", message = "Server Side Error -> Request could not be completed.")
  ))
  def getCompanyById(@ApiParam(value = "An identifier of any type", example = "87395710", required = true) companyNumber: String): Action[AnyContent] = {
    Action.async { implicit request =>
      val src: String = config.getString("source")
      Logger.info(s"Searching for company with id: ${companyNumber} in source: ${src}")
      val res = companyNumber match {
        case companyNumber if checkRegex(companyNumber, "[A-Z]{2}\\d{6}", "[0-9]{8}") => chData.getCompanyById(companyNumber) match {
          case Success(results) => results match {
            case Nil => NotFound(errAsJson(404, "Not Found", s"Could not find value ${companyNumber}")).future
            case _ :: _ :: Nil => InternalServerError(errAsJson(500, "internal server error", s"more than one result returned for companyNumber: $companyNumber")).future
            case x => x.head match {
              case (x: Company) => Ok(Company.toJson(x)).future
              case (y: Unit) => Ok(Unit.toJson(y)).future
            }
          }
          case Failure(e) => InternalServerError(errAsJson(500, "Internal Server Error", s"An error has occurred, please contact the server administrator")).future
        }
        case _ => UnprocessableEntity(errAsJson(422, "Unprocessable Entity", "CompanyNumber should match the following: [A-Z]{2}[0-9]{6} or [0-9]{8}")).future
      }
      res
    }
  }
}