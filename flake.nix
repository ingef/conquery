{
  description = "Local dev shell for Conquery";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-25.05";
    flake-utils.url = "github:numtide/flake-utils";
  };

  outputs = { self, nixpkgs, flake-utils }:
    flake-utils.lib.eachDefaultSystem (system:
      let
        pkgs = import nixpkgs { inherit system; };
      in
      {
        devShells.default = pkgs.mkShell {
          packages = with pkgs; [
            jdk21
            maven
            nodejs_20
            git
            curl
            jq
            ripgrep
            python312
          ];

          shellHook = ''
            export JAVA_HOME=${pkgs.jdk21}
            export PATH="$JAVA_HOME/bin:$PATH"
            echo "Conquery dev shell ready: java=$(java -version 2>&1 | head -n 1), node=$(node -v)"
          '';
        };
      });
}
