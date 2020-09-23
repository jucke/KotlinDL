package api.keras.layers

import api.core.KGraph
import api.keras.util.DATA_PLACEHOLDER
import api.keras.util.getDType
import org.tensorflow.Operand
import org.tensorflow.Shape
import org.tensorflow.op.Ops
import org.tensorflow.op.core.Placeholder

/**
 * First and required layer in [api.keras.Sequential.of] method.
 *
 * @property [name] Custom layer name.
 * @constructor Creates [Input] layer from [packedDims] representing [input] data shape.
 */
class Input(vararg dims: Long, name: String = "") : Layer(name) {
    /** Placeholder for input data. */
    lateinit var input: Placeholder<Float>

    /** Input data dimensions. Rank = 3 or 4 for most popular supported cases. */
    val packedDims: LongArray = dims

    override fun defineVariables(tf: Ops, kGraph: KGraph, inputShape: Shape) {}

    /**
     * Extend this function to define placeholder in layer.
     *
     * NOTE: Called instead of [Layer.defineVariables].
     *
     * @param [tf] TensorFlow graph API for building operations.
     */
    fun defineVariables(tf: Ops) {
        input = tf.withName(DATA_PLACEHOLDER).placeholder(
            getDType(),
            Placeholder.shape(Shape.make(-1L, *packedDims))
        )
    }

    /**
     * Computes output shape, based on [input] and [Layer] type.
     *
     * NOTE: Called instead of [Layer.computeOutputShape].
     */
    fun computeOutputShape(): Shape {
        return input.asOutput().shape()
    }

    override fun transformInput(tf: Ops, input: Operand<Float>): Operand<Float> {
        return input
    }

    override fun computeOutputShape(inputShape: Shape): Shape {
        return inputShape
    }

    override fun getWeights(): List<Array<*>> {
        return emptyList()
    }

    override fun hasActivation(): Boolean {
        return false
    }

    override fun getParams(): Int {
        return 0
    }

    override fun toString(): String {
        return "Input(shape=${packedDims.contentToString()})"
    }
}