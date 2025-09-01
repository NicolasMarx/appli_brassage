package application.queries.public.hops

import domain.hops.model.HopUsage

case class HopListQuery(
  usage: Option[HopUsage] = None,
  page: Int = 0,
  size: Int = 20
)
