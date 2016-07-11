
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
    my %locals;
    for my $src_path(sort <$pkg_dir/*.scala>){
        open SRC, $src_path or die;
        my $content  = join '', <SRC>;
        close SRC or die;

        my $src_abbr = $src_path=~/(\w+)\.scala$/ ? $1 : die;
        my $pf = $src_abbr=~/(I|Mix)$/ ? $1 : "";

        if($pkg_abbr=~/_impl$/){

            if($pf ne "I"){
                $content=~s{\b(class|object|trait)\s+(\w+)}{
                    $locals{$2}{def}{$src_abbr}++;
                    ""
                }egs;
            }
            if($pf ne "Mix"){
                $locals{$_}{'use'}{$src_abbr}++ for $content=~/(\w+)/g;
            }
        }

        $content=~s{\bimport\s+ee\.cone\.base\.(\w+)}{
            my $c = $1;
            $c eq $pkg_abbr ||
            "$c\_impl" eq $pkg_abbr ||
            $globals{"$1 in $pkg_abbr$pf"}++;
            ""
        }egs;
    }
    &$check(\%locals);
}
for(sort keys %globals){
    print "$_\n"
}


#&$check(\%globals);