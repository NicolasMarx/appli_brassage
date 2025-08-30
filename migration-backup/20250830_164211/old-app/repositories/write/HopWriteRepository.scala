package repositories.write

import scala.concurrent.Future
import domain.hops.{Hop, HopDetail, HopId}

/** CQRS write-side pour l'agrégat Hop et ses tables liées.
 * Version alignée sur retours Future[Unit].
 */
trait HopWriteRepository {

  /** Remplace l’ensemble des hops (truncate + insert). */
  def replaceAll(hops: Seq[Hop]): Future[Unit]

  /** Remplace hop_details (truncate + insert). */
  def replaceDetails(details: Seq[HopDetail]): Future[Unit]

  /** Remplace les styles par hop (truncate + insert). */
  def replaceBeerStyles(pairs: Seq[(HopId, String)]): Future[Unit]

  /** Remplace les substituts par hop (truncate + insert). */
  def replaceSubstitutes(pairs: Seq[(HopId, String)]): Future[Unit]

  /** Remplace les liaisons Hop ↔ Aroma (truncate + insert) via IDs d’arômes (slug). */
  def replaceAromas(pairs: Seq[(HopId, String /* aromaId */)]): Future[Unit]
}
