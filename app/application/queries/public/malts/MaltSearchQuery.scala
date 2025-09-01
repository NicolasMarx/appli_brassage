package application.queries.public.malts

import domain.malts.model.MaltType

case class MaltSearchQuery(
  name: Option[String] = None,
  maltType: Option[MaltType] = None,
  minEbc: Option[Double] = None,
  maxEbc: Option[Double] = None,
  minExtraction: Option[Double] = None,
  maxExtraction: Option[Double] = None,
  page: Int = 0,
  size: Int = 20
)
