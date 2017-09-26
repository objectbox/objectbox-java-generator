package io.objectbox.test.kotlin

import io.objectbox.annotation.Backlink
import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import io.objectbox.annotation.Index

@Entity
class Customer {

    @Id
    var id: Long = 0

    @Index
    var name: String? = null

    @Backlink(to = "customerId")
    lateinit var orders: List<Order>

    constructor()

    constructor(id: Long, name: String) {
        this.id = id
        this.name = name
    }

}
