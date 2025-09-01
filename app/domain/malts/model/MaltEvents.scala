package domain.malts.model

import domain.common.BaseDomainEvent
import java.time.Instant

/**
 * Événements de domaine pour l'agrégat MaltAggregate
 * Capture tous les changements d'état significatifs selon Event Sourcing
 * Suit le pattern AdminEvents/HopEvents pour cohérence
 */

case class MaltCreated(
                        maltId: String,
                        name: String,
                        maltType: String,
                        ebcColor: Double,
                        extractionRate: Double,
                        diastaticPower: Double,
                        originCode: String,
                        source: String,
                        createdAt: Instant,
                        override val version: Int
                      ) extends BaseDomainEvent(maltId, "MaltCreated", version)

case class MaltBasicInfoUpdated(
                                 maltId: String,
                                 newName: String,
                                 newDescription: Option[String],
                                 override val version: Int
                               ) extends BaseDomainEvent(maltId, "MaltBasicInfoUpdated", version)

case class MaltSpecsUpdated(
                             maltId: String,
                             newEbcColor: Double,
                             newExtractionRate: Double,
                             newDiastaticPower: Double,
                             override val version: Int
                           ) extends BaseDomainEvent(maltId, "MaltSpecsUpdated", version)

case class MaltFlavorProfilesUpdated(
                                      maltId: String,
                                      newFlavorProfiles: List[String],
                                      override val version: Int
                                    ) extends BaseDomainEvent(maltId, "MaltFlavorProfilesUpdated", version)

case class MaltStatusChanged(
                              maltId: String,
                              previousStatus: String,
                              newStatus: String,
                              reason: String,
                              override val version: Int
                            ) extends BaseDomainEvent(maltId, "MaltStatusChanged", version)

case class MaltCredibilityAdjusted(
                                    maltId: String,
                                    previousScore: Int,
                                    newScore: Int,
                                    reason: String,
                                    override val version: Int
                                  ) extends BaseDomainEvent(maltId, "MaltCredibilityAdjusted", version)

case class MaltActivated(
                          maltId: String,
                          override val version: Int
                        ) extends BaseDomainEvent(maltId, "MaltActivated", version)

case class MaltDeactivated(
                            maltId: String,
                            reason: String,
                            override val version: Int
                          ) extends BaseDomainEvent(maltId, "MaltDeactivated", version)

/**
 * Events pour intégrations et workflows avancés
 */

case class MaltQualityScoreUpdated(
                                    maltId: String,
                                    previousScore: Int,
                                    newScore: Int,
                                    calculationFactors: Map[String, Double], // credibility, completeness, consistency
                                    override val version: Int
                                  ) extends BaseDomainEvent(maltId, "MaltQualityScoreUpdated", version)

case class MaltReviewRequested(
                                maltId: String,
                                reviewReason: String,
                                currentCredibilityScore: Int,
                                requestedBy: String, // Admin ou système IA
                                override val version: Int
                              ) extends BaseDomainEvent(maltId, "MaltReviewRequested", version)

case class MaltCompatibilityAnalyzed(
                                      maltId: String,
                                      beerStylesCompatible: List[String],
                                      analysisTimestamp: Instant,
                                      override val version: Int
                                    ) extends BaseDomainEvent(maltId, "MaltCompatibilityAnalyzed", version)