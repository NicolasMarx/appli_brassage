package application.queries.public.malts

import domain.malts.model.MaltType

case class MaltListQuery(
  maltType: Option[MaltType] = None,
  page: Int = 0,
  size: Int = 20
)
