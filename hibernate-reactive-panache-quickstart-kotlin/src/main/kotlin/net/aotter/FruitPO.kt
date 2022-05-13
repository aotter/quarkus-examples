package net.aotter

import org.hibernate.annotations.GenericGenerator
import javax.persistence.*

@Entity
@Table(name = "fruit")
data class FruitPO(

    @Id
    @GenericGenerator(name = "generator", strategy = "uuid2")
    @GeneratedValue(generator = "generator")
    @Column(name = "fruit_id")
    var fruitId: String? = null,

    @Column(name = "name")
    var name: String? = null,

    )
