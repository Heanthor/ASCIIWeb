require 'open-uri'
a = []
open("http://www.4chan.org/an/") {|src|
	while line = src.gets 
		a = line.scan(/src="\/\/i.4cdn.org\/an\/[a-z\/\.0-9]*"/) # pulls image URLs
	end
}
File.open('images.txt', 'w') {} # clears file
final = ""
a.each{|str|
	final = final + str + "\n"
}
puts final
File.write('images.txt', final) #writes image URLs, newline separated, to file