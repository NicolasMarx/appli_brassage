// app/domain/hops/model/HopEvents.scala
package domain.hops.model

import domain.common.BaseDomainEvent
import java.time.Instant

case class HopCreated(
                       hopId: String,
                       name: String,
                       alphaAcid: Double,
                       originCode: String,
                       usage: String,
                       source: String,
                       createdAt: Instant,
                       override val version: Int
                     ) extends BaseDomainEvent(hopId, "HopCreated", version)

case class HopAlphaAcidUpdated(
                                hopId: String,
                                previousAlphaAcid: Double,
                                newAlphaAcid: Double,
                                updatedUsage: String,
                                override val version: Int
                              ) extends BaseDomainEvent(hopId, "HopAlphaAcidUpdated", version)

case class HopDescriptionUpdated(
                                  hopId: String,
                                  newDescription: String,
                                  override val version: Int
                                ) extends BaseDomainEvent(hopId, "HopDescriptionUpdated", version)

case class HopActivated(
                         hopId: String,
                         override val version: Int
                       ) extends BaseDomainEvent(hopId, "HopActivated", version)

case class HopDeactivated(
                           hopId: String,
                           override val version: Int
                         ) extends BaseDomainEvent(hopId, "HopDeactivated", version)

case class HopCredibilityUpdated(
                                  hopId: String,
                                  previousScore: Int,
                                  newScore: Int,
                                  override val version: Int
                                ) extends BaseDomainEvent(hopId, "HopCredibilityUpdated", version)

case class HopAromaAdded(
                          hopId: String,
                          aromaId: String,
                          override val version: Int
                        ) extends BaseDomainEvent(hopId, "HopAromaAdded", version)

case class HopAromaRemoved(
                            hopId: String,
                            aromaId: String,
                            override val version: Int
                          ) extends BaseDomainEvent(hopId, "HopAromaRemoved", version)