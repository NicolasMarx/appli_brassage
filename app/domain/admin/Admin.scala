package domain.admins

final case class AdminId(value: String)        extends AnyVal
final case class Email(value: String)          extends AnyVal
final case class AdminName(first: String, last: String)
final case class HashedPassword(value: String) extends AnyVal

final case class Admin(
                        id: AdminId,
                        email: Email,
                        name: AdminName,
                        password: HashedPassword
                      )
