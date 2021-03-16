# Parses the release notes file to extract the current version information and
# puts it to STDOUT.
#
# To update the release notes for localization:
#
# ruby ./this_script >| Simplenote/metadata/release_notes.txt
#
# To generate the App Store Connect release message:
#
# ruby ./this_script | pbcopy
#
# To generate the GitHub and App Center release message:
#
# ruby ./this_script -k | pbcopy

RELEASE_NOTES_FILE = 'RELEASE-NOTES.txt'
NOTES = File.read(RELEASE_NOTES_FILE)
lines = NOTES.lines

def replace_pr_link_with_markdown_link(string)
  string.gsub(/\[.*\]$/) do |pr_link|
    url = pr_link[1...-1] # strip the []
    id = url.split('/').last
    "[##{id}](#{url})"
  end
end

# This is a very bare bone option parsing. It does the job for this simple use
# case, but it should not be built upon.
#
# If you plan to add more options, please consider using a gem to manage them
# properly.
mode = ARGV[0] == '-k' ? :keep_pr_links : :strip_pr_links

# Format:
#
# 1.23
# -----
#
# 1.22
# -----
# * something
# * something
#
# 1.21
# -----
# * something something

# Skip the first three lines: the next version header
lines = lines[3...]

# Isolate the current version by looking for the first new line
release_lines = []
lines[2...].each do |line|
  break if line.strip == ''
  release_lines.push line
end

case mode
when :strip_pr_links
  formatted_lines = release_lines.
    map { |l| l.gsub(/ \[.*\]$/, '') }
when :keep_pr_links
  formatted_lines = release_lines.
    # The PR "links" are not canonical markdown links. That's not a problem on
    # GitHub where they be automatically parsed into links to the corresponding
    # PR, but outside GitHub, such as in our internal posts that might be
    # confusing.
    #
    # It's probably best to update the convention in writing the release notes
    # but in the meantime let's compensate with more automation.
    map { |l| replace_pr_link_with_markdown_link(l) }
end

# It would be good to either add overriding of the file where the parsed
# release notes should go. I haven't done it yet because I'm using this script
# also to generate the text for the release notes on GitHub, where I want to
# keep the PR links. See info on the usage a the start of the file.
puts formatted_lines
