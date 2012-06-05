Module module {
    name = 'com.redhat.sample.multisource';
    version = '0.2';
    doc = "Test multi source file compilation";
    license = 'http://www.apache.org/licenses/LICENSE-2.0.html';
    //should have a @error on missing dependency here instead of on name
    //but stuck because of https://github.com/ceylon/ceylon-spec/issues/60
    Import {
        @error name = 'non.existent.module';
        version = '1.0';
    }
}