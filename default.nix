{ pkgs ? import
    (fetchTarball {
      name = "jpetrucciani-2025-12-13";
      url = "https://github.com/jpetrucciani/nix/archive/195a5aebd288ee6934b80497b8ad277feffe5708.tar.gz";
      sha256 = "0ip6789f5csrbmbfpsk416g0llfq09h66hav27agiz9svpf4rvsm";
    })
    { }
}:
let
  name = "PartyTime";
  pg = pkgs.postgresql_16.withPackages (p: with p; [ pgvector ]);

  tools = with pkgs; {
    cli = [
      jfmt
      nixup
    ];
    java = [
      gradle
      zulu25
    ];
    scripts = pkgs.lib.attrsets.attrValues scripts;
  };

  scripts = with pkgs; {
    pg = __pg { postgres = pg; };
    pg_bootstrap = __pg_bootstrap { inherit name; postgres = pg; };
    pg_shell = __pg_shell { inherit name; postgres = pg; };
  };
  paths = pkgs.lib.flatten [ (builtins.attrValues tools) ];
  env = pkgs.buildEnv {
    inherit name paths; buildInputs = paths;
  };
in
(env.overrideAttrs (_: {
  inherit name;
  NIXUP = "0.0.10";
  PGPORT = "7070";
  PGDATA = "${builtins.toString ./.}/.pg/${name}";
  PGDATABASE = name;
  PGUSER = name;
  PGPASSWORD = name;
})) // { inherit scripts; }
