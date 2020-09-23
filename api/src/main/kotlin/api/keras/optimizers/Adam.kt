package api.keras.optimizers

import api.core.KGraph
import api.keras.util.defaultAssignOpName
import api.keras.util.defaultInitializerOpName
import api.keras.util.defaultOptimizerVariableName
import api.keras.util.getDType
import org.tensorflow.Operand
import org.tensorflow.Output
import org.tensorflow.Shape
import org.tensorflow.op.Ops
import org.tensorflow.op.core.Assign
import org.tensorflow.op.core.Constant
import org.tensorflow.op.core.Gradients
import org.tensorflow.op.core.Variable
import java.util.*

private const val FIRST_MOMENT = "m"
private const val SECOND_MOMENT = "v"
private val FIRST_BETA_POWER_NAME = defaultOptimizerVariableName("beta1_power")
private val SECOND_BETA_POWER_NAME = defaultOptimizerVariableName("beta2_power")

/**
 * Adam optimizer.
 *
 * Updates variable according next formula:
 * ```
 * lr_t := learning_rate * sqrt{1 - beta_2^t} / (1 - beta_1^t)
 * m_t := beta_1 * m_{t-1} + (1 - beta_1) * g
 * v_t := beta_2 * v_{t-1} + (1 - beta_2) * g * g
 * variable := variable - lr_t * m_t / sqrt{v_t} + epsilon)
 * ```
 *
 * It is recommended to leave the parameters of this optimizer at their default values.
 *
 *
 * @property [learningRate] Float >= 0. Initial learning rate.
 * @property [beta1] 0 < beta < 1. Generally close to 1.
 * @property [beta2] 0 < beta < 1. Generally close to 1.
 * @property [epsilon] Float >= 0. Fuzz factor.
 */
class Adam(
    private val learningRate: Float = 0.001f,
    private val beta1: Float = 0.9f,
    private val beta2: Float = 0.999f,
    private val epsilon: Float = 1e-07f,
    clipGradient: ClipGradientAction = NoClipGradient()
) : Optimizer(clipGradient) {

    private lateinit var epsilonConstant: Constant<Float>
    private lateinit var learningRateConst: Constant<Float>
    private lateinit var betaOneConst: Constant<Float>
    private lateinit var betaTwoConst: Constant<Float>
    private lateinit var betaOnePower: Variable<Float>
    private lateinit var betaTwoPower: Variable<Float>

    override fun applyGradients(
        graph: KGraph,
        tf: Ops,
        weights: List<Variable<Float>>,
        gradients: Gradients
    ): List<Operand<Float>> {
        val targets: MutableList<Operand<Float>> =
            ArrayList()

        betaOneConst = tf.constant(beta1, getDType())
        betaTwoConst = tf.constant(beta2, getDType())
        learningRateConst = tf.constant(learningRate, getDType())
        epsilonConstant = tf.constant(epsilon, getDType())

        for (i in weights.indices) {

            val variable = weights[i]
            val varName = variable.ref().op().name()

            val firstMomentSlot: Variable<Float> = getSlot(varName, FIRST_MOMENT)
            val secondMomentSlot: Variable<Float> = getSlot(varName, SECOND_MOMENT)

            targets.add(
                tf.train.applyAdam(
                    variable,
                    firstMomentSlot,
                    secondMomentSlot,
                    betaOnePower,
                    betaTwoPower,
                    learningRateConst,
                    betaOneConst,
                    betaTwoConst,
                    epsilonConstant,
                    clipGradient.clipGradient(tf, gradients.dy(i))
                )
            )
        }

        val betaOnePowerInit1 = tf
            //.withName(defaultInitializerOpName(FIRST_BETA_POWER_NAME))
            .assign(betaOnePower, tf.math.mul(betaOnePower, betaOneConst))
        val betaTwoPowerInit2 = tf
            //.withName(defaultInitializerOpName(SECOND_BETA_POWER_NAME))
            .assign(betaTwoPower, tf.math.mul(betaTwoPower, betaTwoConst))

        graph.addOptimizerVariableInitializer(betaOnePowerInit1)
        graph.addOptimizerVariableInitializer(betaTwoPowerInit2)
        graph.addOptimizerVariable(betaOnePower)
        graph.addOptimizerVariable(betaTwoPower)

        return targets
    }

    private fun createAdamSlot(graph: KGraph, tf: Ops, v: Output<Float>) {
        val firstMomentInitializerName = defaultInitializerOpName(createName(v, FIRST_MOMENT))
        val firstMomentInitializer =
            tf.withName(firstMomentInitializerName).fill(tf.shape(v), tf.constant(0.0f, getDType()))
        createSlot(graph, tf, v.asOutput(), FIRST_MOMENT, firstMomentInitializer)

        val secondMomentInitializerName = defaultInitializerOpName(createName(v, SECOND_MOMENT))
        val secondMomentInitializer =
            tf.withName(secondMomentInitializerName).fill(tf.shape(v), tf.constant(0.0f, getDType()))
        createSlot(graph, tf, v.asOutput(), SECOND_MOMENT, secondMomentInitializer)
    }

    override fun createSlots(graph: KGraph, tf: Ops, variables: List<Output<Float>>) {
        for (v in variables) {
            createAdamSlot(graph, tf, v.asOutput())
        }
        betaOnePower = tf.withName(FIRST_BETA_POWER_NAME).variable(Shape.scalar(), getDType())

        val betaOnePowerAssignName = defaultAssignOpName(FIRST_BETA_POWER_NAME)
        val betaOnePowerInit: Assign<*> = tf.withName(betaOnePowerAssignName)
            .assign(
                betaOnePower,
                tf.withName(defaultInitializerOpName(FIRST_BETA_POWER_NAME)).constant(beta1, getDType())
            )
        graph.addOptimizerVariableInitializer(betaOnePowerInit)


        betaTwoPower = tf.withName(SECOND_BETA_POWER_NAME).variable(Shape.scalar(), getDType())

        val betaTwoPowerAssignName = defaultAssignOpName(SECOND_BETA_POWER_NAME)
        val betaTwoPowerInit: Assign<*> = tf.withName(betaTwoPowerAssignName)
            .assign(
                betaTwoPower,
                tf.withName(defaultInitializerOpName(SECOND_BETA_POWER_NAME)).constant(beta2, getDType())
            )
        graph.addOptimizerVariableInitializer(betaTwoPowerInit)
    }

    override fun getOptimizerName(): String {
        return "Adam"
    }
}