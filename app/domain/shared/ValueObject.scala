package domain.shared

/**
 * Trait de base pour tous les Value Objects
 * Garantit l'immutabilité et l'égalité par valeur
 */
trait ValueObject {
  override def equals(obj: Any): Boolean = obj match {
    case that: ValueObject => this.getClass == that.getClass && this.hashCode == that.hashCode
    case _ => false
  }
  
  override def hashCode(): Int = scala.util.hashing.MurmurHash3.productHash(this.asInstanceOf[Product])
  
  def canEqual(other: Any): Boolean = other.getClass == this.getClass
}
