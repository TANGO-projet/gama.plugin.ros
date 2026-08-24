/*******************************************************************************************************
 *
 * GamaROSTopicType.java, in gama.plugin.ros, is part of the source code of the GAMA modeling and simulation platform.
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
 * The GAML type {@code ros_topic}, wrapping {@link GamaROSTopic}.
 *
 * <p>
 * A topic is an immutable description rather than a resource, but casting a string to one is deliberately not
 * supported: the message type has no default, and guessing it would hide the error until the first publication.
 * </p>
 */
@type (
		name = RosConstants.TOPIC,
		id = RosConstants.TOPIC_ID,
		wraps = { GamaROSTopic.class },
		kind = ISymbolKind.REGULAR,
		concept = { RosConstants.CONCEPT },
		doc = { @doc ("The pairing of a topic name with the message type carried on it, obtained with the ros_topic operator. A topic holds no network resource and can be given to any number of nodes.") })
public class GamaROSTopicType extends GamaType<GamaROSTopic> {

	/**
	 * @param typesManager
	 *            the manager the type registers itself with
	 */
	public GamaROSTopicType(final ITypesManager typesManager) {
		super(typesManager);
	}

	@Override
	public GamaROSTopic getDefault() { return null; }

	@Override
	public boolean canCastToConst() { return false; }

	@Override
	public GamaROSTopic cast(final IScope scope, final Object obj, final Object param, final boolean copy)
			throws GamaRuntimeException {
		// nothing else converts to a ros_topic: it is produced by its operator and by nothing else
		return obj instanceof final GamaROSTopic value ? value : null;
	}

}
