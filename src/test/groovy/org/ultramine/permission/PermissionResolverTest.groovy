package org.ultramine.permission

import spock.lang.Specification
import spock.lang.Unroll
import static org.ultramine.permission.PermissionResolver.CheckResult.*

/**
 * Created by Евгений on 08.05.2014.
 */
class PermissionResolverTest extends Specification {

    def setupSpec() {
        PermissionResolver.metaClass.addEntry = { String key, Boolean value, Integer prio ->
            delegate.values.put(key, value)
            delegate.priorities.put(key, prio)
            return delegate
        }
    }

    @Unroll
    def "Test createForKey: #key"() {
        when: "Creating resolver for key"
        def resolver = PermissionResolver.createForKey(key, 0)

        then: "Resolver has this key"
        resolver.check(key) == TRUE

        where:
        key << ["test.key", "super.test.*", "^group.admin"]
    }

    def "Test createInverted"() {
        setup:
        def resolver = new PermissionResolver()
        resolver.addEntry("p.true", true, 0)
        resolver.addEntry("p.false", false, 0)

        when: "Create inverted resolver"
        def inverted = PermissionResolver.createInverted(resolver)

        then: "Permission are inverted"
        inverted.check("p.false") == TRUE
        inverted.check("p.true") == FALSE

        and: "New permissions are not created"
        inverted.check("group.admin") == UNRESOLVED
    }

    def "Test wildcard"() {
        setup: "Resolver with wildcard permission"
        def resolver = new PermissionResolver()
        resolver.addEntry("test.perm.*", true, 0)

        expect: "Other permissions are not affected"
        resolver.check("group.admin") == UNRESOLVED
        resolver.check("group.admin.super") == UNRESOLVED

        and: "Parent nodes are not affected"
        resolver.check("test") == UNRESOLVED
        resolver.check("test.perm") == UNRESOLVED

        and: "Child nodes are affected"
        resolver.check("test.perm.1") == TRUE
        resolver.check("test.perm.2.3") == TRUE
    }

    def "Test single permission override wildcard"() {
        setup: "Resolver with wildcard and permission"
        def resolver = new PermissionResolver()
        resolver.addEntry("test.perm.*", true, 1)
        resolver.addEntry("test.perm.super", false, 0)

        expect: "Wildcard has lower priority"
        resolver.check("test.perm.super") == FALSE
        resolver.check("test.perm.super2") == TRUE

        when: "Invert resolver"
        resolver = PermissionResolver.createInverted(resolver)

        then: "Same effect"
        resolver.check("test.perm.super") == TRUE
        resolver.check("test.perm.super2") == FALSE
    }

    def "Test higher node wildcard priority"() {
        setup: "Resolver with wildcards"
        def resolver = new PermissionResolver()
        resolver.addEntry("test.perm.*", true, 1)
        resolver.addEntry("test.perm.super.*", false, 0)

        expect: "Higher node wildcard has priority"
        resolver.check("test.perm.super.p") == FALSE
        resolver.check("test.perm.super.p.p") == FALSE
        resolver.check("test.perm.p") == TRUE

        when: "Invert resolver"
        resolver = PermissionResolver.createInverted(resolver)

        then: "Same effect"
        resolver.check("test.perm.super.p") == TRUE
        resolver.check("test.perm.super.p.p") == TRUE
        resolver.check("test.perm.p") == FALSE
    }

    def "Test clear"() {
        setup:
        def resolver = new PermissionResolver()
        resolver.addEntry("test.perm", true, 0)

        when: "Clear resolver's data"
        resolver.clear()

        then: "It has no permissions"
        resolver.check("test.perm") == UNRESOLVED
    }

    def "Test merge"() {
        setup: "First resolver"
        def resolver1 = new PermissionResolver()
        resolver1.addEntry("test.perm", true, 1)
        resolver1.addEntry("test.perm.1", true, 1)
        resolver1.addEntry("test.perm.2", false, 1)

        and: "Second resolver"
        def resolver2 = new PermissionResolver()
        resolver2.addEntry("test.perm", false, 0)
        resolver2.addEntry("test.perm.1", false, 2)
        resolver2.addEntry("test.perm.3", true, 2)

        when: "Merging first then second"
        def result = new PermissionResolver()
        result.merge(resolver1, 1)
        result.merge(resolver2, 2)

        then:
        result.check("test.perm") == FALSE
        result.check("test.perm.1") == FALSE
        result.check("test.perm.2") == FALSE
        result.check("test.perm.3") == TRUE
        result.check("group.admin") == UNRESOLVED

        when: "Merge second then first"
        result = new PermissionResolver()
        result.merge(resolver2, 2)
        result.merge(resolver1, 1)

        then: "Same effect"
        result.check("test.perm") == FALSE
        result.check("test.perm.1") == FALSE
        result.check("test.perm.2") == FALSE
        result.check("test.perm.3") == TRUE
        result.check("group.admin") == UNRESOLVED

        when: "Merge first to second"
        resolver2.merge(resolver1, 1)

        then:
        resolver2.check("test.perm") == TRUE
        resolver2.check("test.perm.1") == FALSE
        resolver2.check("test.perm.2") == FALSE
        resolver2.check("test.perm.3") == TRUE
        resolver2.check("group.admin") == UNRESOLVED
    }

    def "Test clear -> merge lower priority"() {
        setup: "resolver(^test, 100)"
        def resolver = new PermissionResolver().addEntry("test", false, 100)
        def inverted = new PermissionResolver().addEntry("test", true, 50)

        when:
        resolver.clear()
        resolver.merge(inverted, 50)

        then:
        resolver.check("test") == TRUE
    }
}
