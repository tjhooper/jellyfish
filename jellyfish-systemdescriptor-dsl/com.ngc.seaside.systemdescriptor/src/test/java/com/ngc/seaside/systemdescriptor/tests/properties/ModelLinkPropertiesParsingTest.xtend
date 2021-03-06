/**
 * UNCLASSIFIED
 *
 * Copyright 2020 Northrop Grumman Systems Corporation
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the
 * Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.ngc.seaside.systemdescriptor.tests.properties

import com.google.inject.Inject
import com.ngc.seaside.systemdescriptor.systemDescriptor.Model
import com.ngc.seaside.systemdescriptor.systemDescriptor.Package
import com.ngc.seaside.systemdescriptor.tests.SystemDescriptorInjectorProvider
import com.ngc.seaside.systemdescriptor.tests.resources.Datas
import com.ngc.seaside.systemdescriptor.tests.resources.Models
import org.eclipse.emf.ecore.resource.Resource
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.XtextRunner
import org.eclipse.xtext.testing.util.ParseHelper
import org.eclipse.xtext.testing.util.ResourceHelper
import org.eclipse.xtext.testing.validation.ValidationTestHelper
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

import static org.junit.Assert.*
import com.ngc.seaside.systemdescriptor.systemDescriptor.SystemDescriptorPackage
import org.junit.Ignore

@RunWith(XtextRunner)
@InjectWith(SystemDescriptorInjectorProvider)
class ModelLinkPropertiesParsingTest {

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
			Models.CLOCK,
			Models.LINKED_CLOCK,
			Datas.ZONED_TIME
		)
		validationTester.assertNoIssues(requiredResources)
	}

	@Test
	def void testDoesParseModelWithLinkProperties() {
		val source = '''
			package clocks.models.part
			
			import clocks.datatypes.ZonedTime
			import clocks.models.part.Clock
			
			model LinkedClock {
			
				input {
					ZonedTime currentTime
				}
			
				parts {
				Clock clock
				Clock clockA
				}
			
				links {
				link namedLink currentTime -> clock.inputTime {
					properties {
						int intField
					}
				}
				link currentTime -> clockA.inputTime {
					properties {
						string stringField
					}
				}
				}
			}
		'''

		val result = parseHelper.parse(source, requiredResources.resourceSet)
		assertNotNull(result)
		validationTester.assertNoIssues(result)

		val model = result.element as Model
		var properties = model.links.declarations.get(0).definition.properties
		assertNotNull("did not parse properties", properties)

		val declaration = properties.declarations.get(0)
		assertEquals("property name not correct", "intField", declaration.name)

		val refinedProperties = model.links.declarations.get(1).definition.properties
		assertNotNull("did not parse properties", refinedProperties)

		val refinedDeclaration = refinedProperties.declarations.get(0)
		assertEquals("property name not correct", "stringField", refinedDeclaration.name)
	}

	@Test
	def void testDoesParseModelWithRefinedLinkProperties() {
		val source = '''
			package clocks.models.part
			
			import clocks.models.part.LinkedClock
			
			model BigClock refines LinkedClock {
				
				parts {
					refine clockA {
					}
				}
			
				links {
					refine namedLink {
						properties {
							int intField
						}
					}
					refine link currentTime -> clockA.inputTime {
						properties {
							string stringField
						}
					}
				}
			}
		'''

		val result = parseHelper.parse(source, requiredResources.resourceSet)
		assertNotNull(result)
		validationTester.assertNoIssues(result)

		val model = result.element as Model
		var properties = model.links.declarations.get(0).definition.properties
		assertNotNull("did not parse properties", properties)

		val declaration = properties.declarations.get(0)
		assertEquals("property name not correct", "intField", declaration.name)

		val refinedProperties = model.links.declarations.get(1).definition.properties
		assertNotNull("did not parse properties", refinedProperties)

		val refinedDeclaration = refinedProperties.declarations.get(0)
		assertEquals("property name not correct", "stringField", refinedDeclaration.name)
	}

	@Test
	def void testDoesParseModelWithRefinedLinkWithNamePropertiesWithValueSet() {
		val source = '''
			package clocks.models.part
			
			import clocks.models.part.LinkedClock
			
			model BigClock refines LinkedClock {
			
			    links {
			        refine propNamedLink {
			            properties {
			                 intLinkedClockField = 1
			            }
			        }
			    }
			}
		'''

		val result = parseHelper.parse(source, requiredResources.resourceSet)
		assertNotNull(result)
		validationTester.assertNoIssues(result)
	}

	@Test
	@Ignore("Bug not fixed; see SystemDescriptorScopeProvider line 273")
	def void testDoesParseModelWithRefinedLinkWithNoNamePropertiesWithValueSet() {
		val source = '''
			package clocks.models.part
			
			import clocks.models.part.LinkedClock
			
			model BigClock refines LinkedClock {
			
			    links {
			        refine link currentTime -> clockA.inputTime {
			        	properties {
			        		anotherIntField = 2
			        	}
			        }
			    }
			}
		'''

		val result = parseHelper.parse(source, requiredResources.resourceSet)
		assertNotNull(result)
		validationTester.assertNoIssues(result)
	}

	@Test
	def void testDoesParseModelWithLinkPropertiesWithValueSet() {
		val source = '''
			package clocks.models.part
			
			import clocks.datatypes.ZonedTime
			import clocks.models.part.Clock
			
			model BigClock {
			    input {
			        ZonedTime currentTime
			    }
			    
			    parts {
			        Clock clock
			    }
			
			    links {
			    link namedLink currentTime -> clock.inputTime {
			        properties {
			            int intField
			                intField = 100
			        }
			    }
			    }
			}
		'''

		val result = parseHelper.parse(source, requiredResources.resourceSet)
		assertNotNull(result)
		validationTester.assertNoIssues(result)
	}

	@Test
	def void testDoesParseModelWithLinkPropertiesWithOverWritenValueSet() {
		val source = '''
			package clocks.models.part
			
			import clocks.models.part.LinkedClock
			
			model AnotherLinkedClock refines LinkedClock {
			
			    links {
			        refine valueNamedLink {
			            properties {
			                intValueClockField = 200 
			            }
			        }
			    }
			}
		'''

		val result = parseHelper.parse(source, requiredResources.resourceSet)
		assertNotNull(result)
		validationTester.assertNoIssues(result)
	}

	@Test
	def void testDoesNotParseModelWithLinkPropertiesWithRedifinedProperty() {
		val source = '''
			package clocks.models.part
			
			import clocks.models.part.LinkedClock
			
			model AnotherLinkedClock refines LinkedClock {
			
			    links {
			        refine valueNamedLink {
			            properties {
			                int intValueClockField
			            }
			        }
			    }
			}
		'''

		val invalidResult = parseHelper.parse(source, requiredResources.resourceSet)
		assertNotNull(invalidResult)
		validationTester.assertError(
			invalidResult,
			SystemDescriptorPackage.Literals.PROPERTIES,
			null
		)
	}

}
