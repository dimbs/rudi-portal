/**
 * RUDI Portail
 */
package org.rudi.facet.bpmn.bean;

import org.rudi.bpmn.core.bean.AssetDescription;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author FNI18300
 *
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class Test2AssetDescription extends AssetDescription {

	private String a;
}
