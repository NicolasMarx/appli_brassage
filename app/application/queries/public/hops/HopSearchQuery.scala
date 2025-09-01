package application.queries.public.hops

import domain.hops.model.HopUsage

case class HopSearchQuery(
  name: Option[String] = None,
  usage: Option[HopUsage] = None,
  minAlphaAcid: Option[Double] = None,
  maxAlphaAcid: Option[Double] = None,
  originCode: Option[String] = None,
  page: Int = 0,
  size: Int = 20
)
