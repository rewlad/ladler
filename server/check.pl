
use strict;
use utf8;

my $src_prefix = "src/main/scala/ee/cone/base/";

my $check = sub{
    my($in)=@_;
    for my $symbol(sort keys %$in){
        my @def = keys %{$$in{$symbol}{def} || next};
        my $def_place = @def == 1 ? $def[0] : die $symbol, @def+0;
        for my $use_place(sort keys %{$$in{$symbol}{'use'}||next}){
            $def_place eq $use_place and next;
            #$def_place=~/(Api|_api|\butil)/ || $use_place=~/(Mix|_mix)/ and next;
            print "$symbol: $def_place -> $use_place\n";
        }
    }
};

my %globals;
for my $pkg_dir(sort <$src_prefix*>){
    my $pkg_abbr = substr $pkg_dir, length $src_prefix;
    my $pkg_pf = $pkg_abbr=~s/(_impl|_mix)$// ? $1 : "";

    my %locals;
    for my $src_path(sort <$pkg_dir/*.scala>){
        open SRC, $src_path or die;
        my $content  = join '', <SRC>;
        close SRC or die;

        if($pkg_pf eq "_impl"){
            my $src_abbr = $src_path=~/(\w+)\.scala$/ ? $1 : die;
            if($src_abbr!~/I$/){
                $content=~s{\b(class|object|trait)\s+(\w+)}{
                    $locals{$2}{def}{$src_abbr}++;
                    ""
                }egs;
            }
            $locals{$_}{'use'}{$src_abbr}++ for $content=~/(\w+)/g;
        }

        $content=~s{\bimport\s+ee\.cone\.base\.(\w+)}{
            my $c = $1;
            my $pf = $c=~s/(_impl|_mix)$// ? $1 : "";
            $pf ne '' && $pkg_pf ne '_mix' ? $globals{"ERR $c $pf in $pkg_abbr $pkg_pf"}++ :
            $c ne $pkg_abbr ? $globals{"rel $pkg_abbr uses $c"}++ : "";
            ""
        }egs;
    }
    &$check(\%locals);
}
for(sort keys %globals){
    print "$_\n"
}


#&$check(\%globals);