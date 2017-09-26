package io.objectbox.test.kotlin

import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import io.objectbox.annotation.NameInDb
import io.objectbox.relation.ToOne
import java.util.*

@Entity
@NameInDb("ORDERS")
class Order {

    @Id(assignable = true)
    var id: Long = 0
    var date: Date? = null
    var customerId: Long = 0
    var text: String? = null

    lateinit var customer: ToOne<Customer>

    lateinit var customerWithoutIdProperty: ToOne<Customer>

}
