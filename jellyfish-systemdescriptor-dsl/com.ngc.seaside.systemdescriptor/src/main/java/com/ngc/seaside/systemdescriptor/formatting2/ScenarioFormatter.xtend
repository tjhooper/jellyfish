/**
 * UNCLASSIFIED
 * Northrop Grumman Proprietary
 * ____________________________
 *
 * Copyright (C) 2018, Northrop Grumman Systems Corporation
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains the property of
 * Northrop Grumman Systems Corporation. The intellectual and technical concepts
 * contained herein are proprietary to Northrop Grumman Systems Corporation and
 * may be covered by U.S. and Foreign Patents or patents in process, and are
 * protected by trade secret or copyright law. Dissemination of this information
 * or reproduction of this material is strictly forbidden unless prior written
 * permission is obtained from Northrop Grumman.
 */
package com.ngc.seaside.systemdescriptor.formatting2

import com.ngc.seaside.systemdescriptor.systemDescriptor.GivenDeclaration
import com.ngc.seaside.systemdescriptor.systemDescriptor.GivenStep
import com.ngc.seaside.systemdescriptor.systemDescriptor.Scenario
import com.ngc.seaside.systemdescriptor.systemDescriptor.ThenDeclaration
import com.ngc.seaside.systemdescriptor.systemDescriptor.ThenStep
import com.ngc.seaside.systemdescriptor.systemDescriptor.WhenDeclaration
import com.ngc.seaside.systemdescriptor.systemDescriptor.WhenStep
import org.eclipse.xtext.formatting2.IFormattableDocument
import com.ngc.seaside.systemdescriptor.systemDescriptor.SystemDescriptorPackage
import com.ngc.seaside.systemdescriptor.systemDescriptor.Step
import org.eclipse.xtext.formatting2.regionaccess.ISemanticRegion

class ScenarioFormatter extends AbstractSystemDescriptorFormatter {
	def dispatch void format(Scenario scenario, extension IFormattableDocument document) {
		var begin = scenario.regionFor.keyword('scenario')
		var end = scenario.regionFor.keyword('}')
		interior(begin, end)[indent]

		scenario.regionFor.feature(SystemDescriptorPackage.Literals.SCENARIO__NAME)
			.prepend[newLines = 0; oneSpace]
			.append[oneSpace]

		scenario.regionFor.keyword('{').prepend[oneSpace].append[newLine]

		scenario.metadata?.format

		scenario.given?.format
		scenario.when?.format
		scenario.then?.format

		scenario.append[setNewLines = 2]
	}

	def dispatch void format(GivenDeclaration given, extension IFormattableDocument document) {
		for (GivenStep step : given.steps) {
			given.append[newLine]
			step.format
		}

		given.append[newLine]
	}

	def dispatch void format(WhenDeclaration when, extension IFormattableDocument document) {
		for (WhenStep step : when.steps) {
			when.append[newLine]
			step.format
		}

		when.append[newLine]
	}

	def dispatch void format(ThenDeclaration then, extension IFormattableDocument document) {
		for (ThenStep step : then.steps) {
			then.append[newLine]
			step.format
		}

		then.append[newLine]
	}

	def dispatch void format(Step step, extension IFormattableDocument document) {
		step.append[newLine]
		step.regionFor.feature(SystemDescriptorPackage.Literals.STEP__KEYWORD)
			.prepend[newLines = 0; oneSpace]
			.append[oneSpace]

		for (ISemanticRegion param : step.regionFor.features(SystemDescriptorPackage.Literals.STEP__PARAMETERS)) {
			param.append[oneSpace]
		}
	}
}
