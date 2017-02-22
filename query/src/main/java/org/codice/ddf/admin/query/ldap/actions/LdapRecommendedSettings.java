package org.codice.ddf.admin.query.ldap.actions;

import java.util.List;
import java.util.Map;

import org.codice.ddf.admin.query.api.fields.Field;
import org.codice.ddf.admin.query.commons.actions.DefaultAction;
import org.codice.ddf.admin.query.ldap.fields.LdapConnectionField;
import org.codice.ddf.admin.query.ldap.fields.LdapCredentialsField;
import org.codice.ddf.admin.query.ldap.fields.LdapSettingsField;

import com.google.common.collect.ImmutableList;

public class LdapRecommendedSettings extends DefaultAction<LdapSettingsField> {

    public static final String ACTION_ID = "recommendedSettings";
    public static final String DESCRIPTION = "Attempts to retrieve recommended settings from the LDAP connection.";
    public static final List<Field> REQUIRED_FIELDS = ImmutableList.of(new LdapConnectionField(), new LdapCredentialsField());
    public LdapRecommendedSettings() {
        super(ACTION_ID, DESCRIPTION, REQUIRED_FIELDS, null, new LdapSettingsField());
    }

    @Override
    public LdapSettingsField process(Map<String, Object> args) {
        return new LdapSettingsField().userBaseDn("exampleUserBaseDn")
                .groupBaseDn("exampleBaseDn")
                .groupObjectClass("exampleGroupObjectClass")
                .groupMembershipAttribute("exampleMembershipAttribute")
                .usernameAttribute("exampleUsernameAttribute")
                .mappingEntry("exampleClaim", "exampleAttribute")
                .mappingEntry("exampleClaim2", "exampleAttribute2");
    }
}
