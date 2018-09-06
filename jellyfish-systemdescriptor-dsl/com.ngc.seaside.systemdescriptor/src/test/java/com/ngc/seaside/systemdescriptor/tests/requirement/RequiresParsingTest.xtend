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
package com.ngc.seaside.systemdescriptor.tests.requirement

import com.google.inject.Inject
import com.ngc.seaside.systemdescriptor.systemDescriptor.Model
import com.ngc.seaside.systemdescriptor.systemDescriptor.Package
import com.ngc.seaside.systemdescriptor.systemDescriptor.SystemDescriptorPackage
import org.eclipse.emf.ecore.resource.Resource
import org.eclipse.xtext.diagnostics.Diagnostic
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.XtextRunner
import org.eclipse.xtext.testing.util.ParseHelper
import org.eclipse.xtext.testing.util.ResourceHelper
import org.eclipse.xtext.testing.validation.ValidationTestHelper
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

import static org.junit.Assert.*
import com.ngc.seaside.systemdescriptor.tests.SystemDescriptorInjectorProvider
import com.ngc.seaside.systemdescriptor.tests.resources.Models
import com.ngc.seaside.systemdescriptor.tests.resources.Datas
import com.ngc.seaside.systemdescriptor.systemDescriptor.BaseRequireDeclaration

@RunWith(XtextRunner)
@InjectWith(SystemDescriptorInjectorProvider)
class RequiresParsingTest {

	@Inject
	ParseHelper<Package> parseHelper

	@Inject
	ResourceHelper resourceHelper

	@Inject
	ValidationTestHelper validationTester

	Resource requiredResources

	@Before
	def void setup() {
		requiredResources = Models.allOf(
			resourceHelper,
			Models.ALARM,
			Models.CLOCK,
			Models.SPEAKER,
			Datas.ZONED_TIME,
			Datas.TIME_ZONE,
			Datas.DATE_TIME,
			Datas.DATE,
			Datas.TIME
		)
		validationTester.assertNoIssues(requiredResources)
	}

	@Test
	def void testDoesParseModelWithRequires() {
		val source = '''
			package clocks.models
			
			import clocks.models.part.Speaker
			
			model Alarm {
				
				requires {
				    Speaker speaker
				 }
			}
		'''

		val result = parseHelper.parse(source, requiredResources.resourceSet)
		assertNotNull(result)
		validationTester.assertNoIssues(result)

		val model = result.element as Model
		val requirements = model.requires
		assertNotNull(
			"did not parse requires!",
			requirements
		)

		val require = requirements.declarations.get(0)
		assertEquals(
			"require name not correct!",
			"speaker",
			require.name
		)
		assertEquals(
			"require type not correct!",
			"Speaker",
			(require as BaseRequireDeclaration).type.name
		)
	}
	
	@Test
	def void testDoesParseModelWithRefinedRequires() {
		val source = '''
			package clocks.models
			
			import clocks.models.part.Clock
			import clocks.datatypes.TimeZone
			
			model BigClock refines Clock{
				
				requires {
					refine requiresEmptyModel
				}
				
				properties {
					releaseDate.dataTime.date.year = 1
					releaseDate.dataTime.date.month = 1
					releaseDate.dataTime.date.day = 1
					releaseDate.timeZone = TimeZone.CST
				}
			}
		'''

		val result = parseHelper.parse(source, requiredResources.resourceSet)
		assertNotNull(result)
		validationTester.assertNoIssues(result)

	}

	@Test
	def void testDoesNotParseModelWithRequiresOfTypeData() {
		val source = '''
			package clocks.models
			
			import clocks.datatypes.Time
			
			model Alarm {
				
				requires {
					Time time
				}
			}
		'''

		val invalidResult = parseHelper.parse(source, requiredResources.resourceSet)
		assertNotNull(invalidResult)
		validationTester.assertError(
			invalidResult,
			SystemDescriptorPackage.Literals.REQUIRE_DECLARATION,
			Diagnostic.LINKING_DIAGNOSTIC
		)
	}

	@Test
	def void testDoesNotParseModelWithDuplicateRequires() {
		val source = '''
			package clocks.models
			
			import clocks.models.part.Speaker
			
			model Alarm {
				
				requires {
					Speaker speaker
					Speaker speaker
				}
			}
		'''

		val invalidResult = parseHelper.parse(source, requiredResources.resourceSet)
		assertNotNull(invalidResult)
		validationTester.assertError(
			invalidResult,
			SystemDescriptorPackage.Literals.REQUIRE_DECLARATION,
			null
		)
	}

	@Test
	def void testDoesNotParseModelWithDuplicateRequireAndInput() {
		val source = '''
			package clocks.models
			
			import clocks.models.part.Speaker
			import clocks.datatypes.Time
			
			model Alarm {
				
				requires {
					Speaker speaker
				}
				
				input {
					Time speaker
				}
			}
		'''

		val invalidResult = parseHelper.parse(source, requiredResources.resourceSet)
		assertNotNull(invalidResult)
		validationTester.assertError(
			invalidResult,
			SystemDescriptorPackage.Literals.INPUT_DECLARATION,
			null
		)
	}

	@Test
	def void testDoesNotParseModelWithDuplicateRequireAndOutput() {
		val source = '''
			package clocks.models
			
			import clocks.models.part.Speaker
			import clocks.datatypes.Time
			
			model Alarm {
				
				requires {
					Speaker speaker
				}
				
				output {
					Time speaker
				}
			}
		'''

		val invalidResult = parseHelper.parse(source, requiredResources.resourceSet)
		assertNotNull(invalidResult)
		validationTester.assertError(
			invalidResult,
			SystemDescriptorPackage.Literals.OUTPUT_DECLARATION,
			null
		)
	}

	@Test
	def void testDoesNotParseModelWithDuplicateRequireAndPart() {
		val source = '''
			package clocks.models
			
			import clocks.models.part.Speaker
			import clocks.datatypes.Time
			
			model Alarm {
				
				requires {
					Speaker speaker
				}
				
				parts {
					Speaker speaker
				}
			}
		'''

		val invalidResult = parseHelper.parse(source, requiredResources.resourceSet)
		assertNotNull(invalidResult)
		validationTester.assertError(
			invalidResult,
			SystemDescriptorPackage.Literals.PART_DECLARATION,
			null
		)
	}

	@Test
	def void testDoesNotParseModelWithEscapedRequiresFieldName() {
		val source = '''
			package clocks.models
			
			import clocks.models.part.Speaker
			
			model Alarm {
				
				requires {
				    Speaker ^int
				 }
			}
		'''

		val invalidResult = parseHelper.parse(source, requiredResources.resourceSet)
		assertNotNull(invalidResult)
		validationTester.assertError(
			invalidResult,
			SystemDescriptorPackage.Literals.REQUIRE_DECLARATION,
			null
		)
	}
	
	@Test
	def void testDoesNotParseANonRefinedModelThatRefinesARequirement() {
		val source = '''
			package clocks.models
			
			model BigClock {
			
				requires {
					refine emptyModel
				}
			}
		     '''

		var invalidResult = parseHelper.parse(source, requiredResources.resourceSet)
		assertNotNull(invalidResult)

		validationTester.assertError(invalidResult, SystemDescriptorPackage.Literals.REQUIRE_DECLARATION, null)
	}
	
	@Test
	def void testDoesNotParseRefinedModelOfARequireDeclarationThatWasntInTheRefinedModel() {
		val source = '''
			package clocks.models
			
			import clocks.models.part.Clock
			
			model BigClock refines Clock{
			
				requires {
					refine superModel
				}
			}
		     '''

		var invalidResult = parseHelper.parse(source, requiredResources.resourceSet)
		assertNotNull(invalidResult)

		validationTester.assertError(invalidResult, SystemDescriptorPackage.Literals.REQUIRE_DECLARATION, null)
	}
}
