with import <nixpkgs> {};
    with pkgs.python27Packages;
    
    buildPythonPackage { 
      name = "impurePythonEnv";
      buildInputs = [
         git
         pystache
         libxml2
         libxslt
         libzip
         python27Full
         python27Packages.virtualenv
         stdenv
         zlib ];
      src = null;
      # When used as `nix-shell --pure`
      shellHook = ''
      unset http_proxy
      export GIT_SSL_CAINFO=/etc/ssl/certs/ca-bundle.crt
      source env/bin/activate
      true
      '';
      # used when building environments
      extraCmds = ''
      unset http_proxy # otherwise downloads will fail ("nodtd.invalid")
      export GIT_SSL_CAINFO=/etc/ssl/certs/ca-bundle.crt
      '';
    }
