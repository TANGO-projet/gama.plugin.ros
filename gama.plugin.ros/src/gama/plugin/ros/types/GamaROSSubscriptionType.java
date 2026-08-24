/*******************************************************************************************************
 *
 * GamaROSSubscriptionType.java, in gama.plugin.ros, is part of the source code of the GAMA modeling and simulation platform.
 *
 * (c) 2007-2026 UMI 209 UMMISCO IRD/SU & Partners (IRIT, MIAT, ESPACE-DEV, CTU)
 *
 * Visit https://github.com/TANGO-projet/gama.plugin.ros for license information and contacts.
 *
 ********************************************************************************************************/
package gama.plugin.ros.types;

import gama.annotations.doc;
import gama.annotations.type;
import gama.annotations.support.ISymbolKind;
import gama.api.exceptions.GamaRuntimeException;
import gama.api.gaml.types.GamaType;
import gama.api.gaml.types.ITypesManager;
import gama.api.runtime.scope.IScope;
import gama.plugin.ros.RosConstants;

/**
 * The GAML type {@code ros_subscription}, wrapping {@link GamaROSSubscription}.
 *
 * <p>
 * A subscription is a native resource owning one queue: it is never copied, since two copies sharing a queue would
 * steal each other's messages.
 * </p>
 */
@type (
		name = RosConstants.SUBSCRIPTION,
		id = RosConstants.SUBSCRIPTION_ID,
		wraps = { GamaROSSubscription.class },
		kind = ISymbolKind.REGULAR,
		concept = { RosConstants.CONCEPT },
		doc = { @doc ("A reader on one topic of one node, obtained with the ros_subscription operator. It buffers the messages that arrive between two cycles of the simulation, which ros_read, ros_read_latest and ros_read_all take out of the queue.") })
public class GamaROSSubscriptionType extends GamaType<GamaROSSubscription> {

	/**
	 * @param typesManager
	 *            the manager the type registers itself with
	 */
	public GamaROSSubscriptionType(final ITypesManager typesManager) {
		super(typesManager);
	}

	@Override
	public GamaROSSubscription getDefault() { return null; }

	@Override
	public boolean canCastToConst() { return false; }

	@Override
	public GamaROSSubscription cast(final IScope scope, final Object obj, final Object param, final boolean copy)
			throws GamaRuntimeException {
		// nothing else converts to a ros_subscription: it is produced by its operator and by nothing else
		return obj instanceof final GamaROSSubscription value ? value : null;
	}

}
