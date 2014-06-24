package play.api

import play.api.libs.json._

package object hal {

  case class HalResource(links: HalLinks, state: JsObject, embedded: Vector[(String, Vector[HalResource])] = Vector.empty) {
    def ++(other: HalResource): HalResource = {
      val d = state ++ other.state
      val l = links ++ other.links
      val e = embedded ++ other.embedded
      HalResource(l, d, e)
    }

    def include(other: HalResource) = ++(other)

    def ++(link: HalLink): HalResource = {
      this.copy(links = links ++ link)
    }

    def include(link: HalLink) = ++(link)
  }

  case class HalLink(name: String, href: String, templated: Boolean = false)

  object HalLinks {
    def empty = HalLinks(Vector.empty)
  }

  case class HalLinks(links: Vector[HalLink]) {
    def ++(other: HalLinks) = {
      HalLinks(links ++ other.links)
    }

    def include(other: HalLinks) = ++(other)

    def ++(link: HalLink) = HalLinks(link +: this.links)

    def include(link: HalLink) = ++(link)
  }

  implicit val halLinkWrites = new Writes[HalLinks] {
    def writes(hal: HalLinks): JsValue = {
      val halLinks = hal.links.map { link =>
        val href = Json.obj("href" -> JsString(link.href))
        val links = if (link.templated) href + ("templated" -> JsBoolean(true)) else href
        link.name -> links
      }
      Json.obj("_links" -> JsObject(halLinks))
    }
  }

  implicit val halResourceWrites: Writes[HalResource] = new Writes[HalResource] {
    def writes(hal: HalResource): JsValue = {

      val embedded = toEmbeddedJson(hal)

      val resource = if (hal.links.links.isEmpty) hal.state
      else Json.toJson(hal.links).as[JsObject] ++ hal.state
      if (embedded.fields.isEmpty) resource
      else resource + ("_embedded" -> embedded)
    }

    def toEmbeddedJson(hal: HalResource): JsObject = {
      hal.embedded match {
        case Vector((k, Vector(elem))) => Json.obj((k, Json.toJson(elem)))
        case e if e.isEmpty => JsObject(Nil)
        case e => JsObject(e.map {
          case (link, resources) =>
            link -> Json.toJson(resources.map(r => Json.toJson(r)))
        })
      }
    }
  }
}
