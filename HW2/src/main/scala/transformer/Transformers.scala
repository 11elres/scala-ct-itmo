package transformer

object Transformers {

  val duplicate: String => String =
    (str: String) => str + str

  val divide: String => String =
    (str: String) => str.substring(0, str.length / 2)

  val revert: String => String =
    _.reverse

  val closure: String => (String => String) => String =
    (str: String) => (transformer: String => String) => transformer(str)

}
