/*******************************************************************************************************
 *
 * GamaROSPublisherType.java, in gama.plugin.ros, is part of the source code of the GAMA modeling and simulation platform.
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
 * The GAML type {@code ros_publisher}, wrapping {@link GamaROSPublisher}.
 *
 * <p>
 * A publisher is a native resource owned by its node: it is never copied, and it is destroyed either by
 * {@code ros_close} or by the closing of its node.
 * </p>
 */
@type (
		name = RosConstants.PUBLISHER,
		id = RosConstants.PUBLISHER_ID,
		wraps = { GamaROSPublisher.class },
		kind = ISymbolKind.REGULAR,
		concept = { RosConstants.CONCEPT },
		doc = { @doc ("A writer on one topic of one node, obtained with the ros_publisher operator and used by ros_publish.") })
public class GamaROSPublisherType extends GamaType<GamaROSPublisher> {

	/**
	 * @param typesManager
	 *            the manager the type registers itself with
	 */
	public GamaROSPublisherType(final ITypesManager typesManager) {
		super(typesManager);
	}

	@Override
	public GamaROSPublisher getDefault() { return null; }

	@Override
	public boolean canCastToConst() { return false; }

	@Override
	public GamaROSPublisher cast(final IScope scope, final Object obj, final Object param, final boolean copy)
			throws GamaRuntimeException {
		// nothing else converts to a ros_publisher: it is produced by its operator and by nothing else
		return obj instanceof final GamaROSPublisher value ? value : null;
	}

}
