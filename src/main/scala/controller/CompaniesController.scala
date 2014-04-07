package controller

import skinny._
import skinny.validator._
import java.util.Locale
import model.Company

/**
 * see also: controller.Controllers
 */
class CompaniesController extends ApplicationController {
  protectFromForgery()

  /**
   * Creates validator with prefix(resourceName).
   */
  override def validation(params: Params, validations: NewValidation*)(
    implicit locale: Locale = currentLocale.orNull[Locale]): MapValidator = {
    validationWithPrefix(params, "company", validations: _*)
  }

  protected def enablePagination: Boolean = true

  /**
   * Shows a list of resource.
   *
   * GET /{resources}
   * GET /{resources}?pageNo=1&pageSize=10
   */
  def showResources()(implicit format: Format = Format.HTML): Any = withFormat(format) {
    if (enablePagination) {
      val pageNo: Int = params.getAs[Int]("page").getOrElse(1)
      val pageSize: Int = 20
      val totalCount: Long = Company.count()
      val totalPages: Int = (totalCount / pageSize).toInt + (if (totalCount % pageSize == 0) 0 else 1)

      set("items", Company.findAllWithPagination(Pagination.page(pageNo).per(pageSize)))
      set("totalPages" -> totalPages)
    } else {
      set("items", Company.findAll())
    }
    render(s"/companies/index")
  }

  /**
   * Show single resource.
   *
   * GET /{resources}/{id}
   */
  def showResource(id: Long)(implicit format: Format = Format.HTML): Any = withFormat(format) {
    set("item", findResource(id).getOrElse(haltWithBody(404)))
    render(s"/companies/show")
  }

  def findResource(id: Long): Option[Company] = Company.findById(id)

  /**
   * Shows input form for creation.
   *
   * GET /{resources}/new
   */
  def newResource()(implicit format: Format = Format.HTML): Any = withFormat(format) {
    render(s"/companies/new")
  }

  /**
   * Params for creation.
   */
  def createParams: Params = Params(params)

  /**
   * Input form for creation.
   */
  def createForm = validation(createParams,
    paramKey("name") is required & maxLength(512),
    paramKey("url") is required & maxLength(512)
  )

  /**
   * Strong parameter definitions for creation form.
   */
  def createFormStrongParameters = Seq(
    "name" -> ParamType.String,
    "url" -> ParamType.String
  )

  /**
   * Creates new resource.
   *
   * POST /{resources}
   */
  def createResource()(implicit format: Format = Format.HTML): Any = withFormat(format) {
    debugLoggingParameters(createForm)
    if (createForm.validate()) {
      val id = {
        val parameters = createParams.permit(createFormStrongParameters: _*)
        debugLoggingPermittedParameters(parameters)
        Company.createWithPermittedAttributes(parameters)
      }
      flash += ("notice" -> createI18n().get("company.flash.created").getOrElse("The company was created."))
      redirect302(s"/companies/${id}")
    } else {
      status = 400
      render("/companies/new")
    }
  }

  /**
   * Shows input form for modification.
   *
   * GET /{resources}/{id}/edit
   */
  def editResource(id: Long)(implicit format: Format = Format.HTML): Any = withFormat(format) {
    Company.findById(id).map { m =>
      status = 200
      setAsParams(m)
      render("/companies/edit")
    } getOrElse haltWithBody(404)
  }

  /**
   * Params for modification.
   */
  def updateParams = Params(params)

  /**
   * Input form for modification.
   */
  def updateForm = validation(updateParams,
    paramKey("name") is required & maxLength(512),
    paramKey("url") is required & maxLength(512)
  )

  /**
   * Strong parameter definitions for modification form.
   */
  def updateFormStrongParameters = Seq(
    "name" -> ParamType.String,
    "url" -> ParamType.String
  )

  /**
   * Updates the specified single resource.
   *
   * POST|PUT|PATCH /{resources}/{id}
   */
  def updateResource(id: Long)(implicit format: Format = Format.HTML): Any = withFormat(format) {
    debugLoggingParameters(updateForm, Some(id))
    Company.findById(id).map { m =>
      if (updateForm.validate()) {
        val parameters = updateParams.permit(updateFormStrongParameters: _*)
        debugLoggingPermittedParameters(parameters, Some(id))
        Company.updateById(id).withPermittedAttributes(parameters)
        status = 200
        flash += ("notice" -> createI18n().get("company.flash.updated").getOrElse("The company was updated."))
        set("item", Company.findById(id).getOrElse(haltWithBody(404)))
        redirect302(s"/companies/${id}")
      } else {
        status = 400
        render("/companies/edit")
      }
    } getOrElse haltWithBody(404)
  }

  /**
   * Destroys the specified resource.
   *
   * DELETE /{resources}/{id}
   */
  def destroyResource(id: Long)(implicit format: Format = Format.HTML): Any = withFormat(format) {
    Company.findById(id).map { m =>
      Company.deleteById(id)
      flash += ("notice" -> createI18n().get("company.flash.deleted").getOrElse("The company was deleted."))
      status = 200
    } getOrElse haltWithBody(404)
  }

  // debug logging

  protected def debugLoggingParameters(form: MapValidator, id: Option[Long] = None) = {
    val forId = id.map { id => s" for [id -> ${id}]" }.getOrElse("")
    val params = form.paramMap.map { case (name, value) => s"${name} -> '${value}'" }.mkString("[", ", ", "]")
    logger.debug(s"Parameters${forId}: ${params}")
  }

  protected def debugLoggingPermittedParameters(parameters: PermittedStrongParameters, id: Option[Long] = None) = {
    val forId = id.map { id => s" for [id -> ${id}]" }.getOrElse("")
    val params = parameters.params.map { case (name, (v, t)) => s"${name} -> '${v}' as ${t}" }.mkString("[", ", ", "]")
    logger.debug(s"Permitted parameters${forId}: ${params}")
  }

}
