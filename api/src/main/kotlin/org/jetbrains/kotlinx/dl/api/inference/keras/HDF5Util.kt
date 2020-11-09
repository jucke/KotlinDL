/*
 * Copyright 2020 JetBrains s.r.o. and Kotlin Deep Learning project contributors. All Rights Reserved.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.kotlinx.dl.api.inference.keras

import io.jhdf.HdfFile
import io.jhdf.api.Group

/**
 * Helper function to print out file in hdf5 format for debugging purposes.
 */
public fun recursivePrintGroup(hdfFile: HdfFile, group: Group, level: Int) {
    var level = level
    level++

    var indent = ""

    for (i in 1..level) {
        indent += "    "
    }

    for (node in group) {
        println(indent + node.name)

        for (entry in node.attributes.entries) {
            println(entry.value)
        }

        if (node is Group) {
            recursivePrintGroup(hdfFile, node, level)
        } else {
            println(node.path)
            val dataset = hdfFile.getDatasetByPath(node.path)
            val dims = arrayOf(dataset.dimensions)
            println(dims.contentDeepToString())

            /*when (dataset.dimensions.size) {
                4 -> {
                    val data = dataset.data as Array<Array<Array<FloatArray>>>
                    println(data.contentDeepToString())
                }
                3 -> {
                    val data = dataset.data as Array<Array<FloatArray>>
                    println(data.contentDeepToString())
                }
                2 -> {
                    val data = dataset.data as Array<FloatArray>
                    println(data.contentDeepToString())
                }
                1 -> {
                    val data = dataset.data as FloatArray
                    println(data.contentToString())
                }
            }*/
        }
    }
}
