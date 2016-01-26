
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
            $def_place=~/(Api|_api|\butil)/ || $use_place=~/(Mix|_mix)/ and next;
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
        $content=~s{\b(class|object|trait)\s+(\w+)}{
            $locals{$2}{def}{$src_abbr}++;
            ""
        }egs;
        $locals{$_}{'use'}{$src_abbr}++ for $content=~/(\w+)/g;

        $globals{$pkg_abbr}{def}{$pkg_abbr}++;
        $content=~s{\bimport\s+io\.github\.rewlad\.ladler\.(\w+)}{
            $globals{$1}{'use'}{$pkg_abbr}++;
            ""
        }egs;
    }
    &$check(\%locals);
}
&$check(\%globals);