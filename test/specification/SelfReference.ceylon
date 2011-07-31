interface SelfReference {
    
    void use(Object o) {}
    class Super(Object o) {}
    
    class Good() extends Super("hello") {
        use("hello");
        function f() {
            use(this);
            return this;
        }
        value v {
            use(this);
            return this;
        }
        class Inner() 
                extends Super(outer) {
            function f() {
                use(outer);
                return outer;
            }
            value v {
                use(outer);
                return outer;
            }
        }
    }
    
    class Good2() extends Super("hello") {
        function f() {
            use(this);
            return this;
        }
        value v {
            use(this);
            return this;
        }
        class Inner() 
                extends Super(outer) {
            function f() {
                use(outer);
                return outer;
            }
            value v {
                use(outer);
                return outer;
            }
        }
    }
    
    @error class Bad() extends Super(this) {
        function f() {
            @error use(this);
            @error return this;
        }
        value v {
            @error use(this);
            @error return this;
        }
        @error class Inner() 
                extends Super(outer) {
            function f() {
                @error use(outer);
                @error return outer;
            }
            value v {
                @error use(outer);
                @error return outer;
            }
        }
        use("hello");
    }
    
}