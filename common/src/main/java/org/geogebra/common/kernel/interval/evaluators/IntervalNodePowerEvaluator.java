package org.geogebra.common.kernel.interval.evaluators;

import static org.geogebra.common.kernel.interval.IntervalConstants.undefined;
import static org.geogebra.common.kernel.interval.operators.IntervalOperationImpl.pow;

import org.geogebra.common.kernel.Kernel;
import org.geogebra.common.kernel.arithmetic.MyDouble;
import org.geogebra.common.kernel.interval.Interval;
import org.geogebra.common.kernel.interval.node.IntervalExpressionNode;
import org.geogebra.common.kernel.interval.node.IntervalNode;
import org.geogebra.common.kernel.interval.node.IntervalOperation;
import org.geogebra.common.kernel.interval.operators.IntervalOperationImpl;
import org.geogebra.common.util.debug.Log;

/**
 * Class to evaluate expressions on an interval that has power in it.
 */
public class IntervalNodePowerEvaluator {

	public Interval handle(Interval base, Interval exponent, IntervalNode right) {
		if (exponent.isUndefined()) {
			return undefined();
		}

		if (MyDouble.exactEqual(base.getLow(), Math.E)) {
			return IntervalOperationImpl.exp(exponent);
		}

		if (!base.isPositive() && right.asExpressionNode() != null) {
			try {
				Interval negPower = calculateNegPower(right.asExpressionNode(), base);
				if (!negPower.isUndefined()) {
					return negPower;
				}
			} catch (Exception e) {
				Log.debug(e);
			}
		}

		return pow(base, exponent);
	}

	private Interval calculateNegPower(IntervalExpressionNode node, Interval base) {
		if (isPositiveFraction(node)) {
			return negativePower(base, node);
		} else if (isNegativeFraction(node)) {
			return negativePower(base, node.getRight().asExpressionNode())
					.multiplicativeInverse();
		}

		return undefined();
	}

	private boolean isPositiveFraction(IntervalExpressionNode node) {
		return node.isOperation(IntervalOperation.DIVIDE);
	}

	private boolean isNegativeFraction(IntervalExpressionNode node) {
		return node.isOperation(IntervalOperation.MULTIPLY)
				&& isMinusOne(node.getLeft())
				&& node.getRight().asExpressionNode().isOperation(IntervalOperation.DIVIDE);
	}

	private boolean isMinusOne(IntervalNode node) {
		return node != null && node.value().isMinusOne();
	}

	private Interval negativePower(Interval base, IntervalExpressionNode node) {
		Interval nominator = node.getLeft().value();
		if (nominator.isSingletonInteger()) {
			Interval denominator = node.getRight().value();
			if (denominator.isUndefined()) {
				return undefined();
			} else if (denominator.isSingletonInteger()) {
				return powerFraction(base, (long) nominator.getLow(),
						(long) denominator.getLow());

			}
		}
		return undefined();
	}

	private Interval powerFraction(Interval x, long a, long b) {
		Interval posPower = powerFractionPositive(x, Math.abs(a), Math.abs(b));
		posPower.setInverted(x.isInverted());
		if (a * b < 0) {
			return posPower.multiplicativeInverse();
		} else {
			return posPower;
		}
	}

	private Interval powerFractionPositive(Interval x, long a, long b) {
		long gcd = Kernel.gcd(a, b);
		if (gcd == 0) {
			return undefined();
		}

		long nominator = a / gcd;
		long denominator = b / gcd;
		Interval interval = new Interval(x);
		Interval base = nominator == 1
				? interval
				: pow(interval, nominator);

		if (base.isPositiveWithZero()) {
			return pow(base, 1d / denominator);
		}
		if (base.contains(0)) {
			if (isOdd(denominator)) {
				return new Interval(-Math.pow(-base.getLow(), 1d / denominator),
						Math.pow(base.getHigh(), 1d / denominator));
			}

			return pow(new Interval(0, base.getHigh()), 1d / denominator);
		}
		if (isOdd(denominator)) {
			return pow(base.negative(), 1d / denominator).negative();
		}

		return undefined();
	}

	private boolean isOdd(long value) {
		return (Math.abs(value) % 2) == 1;
	}
}
