require 'pg'
require 'net/ssh'
require 'json'

# TODO read from config.json?
$config = {
  :db => {
    :container_name      => 'ppd-db',
    :prod_container_name => 'puffpuffdash_db_1',
    :image               => 'postgres',
    :username            => 'postgres',
    :password            => 'secretlol',
    :database            => 'ppd',
    :host                => 'localhost',
    :host_port           => 6432
  },
  :services => {
    :server   => 'lein run',
    :figwheel => 'lein figwheel'
  },
  :deploy => {
    :db => {
      :container_name => 'puffpuffdash_db_1',
      :username       => 'postgres',
      :password       => 'secretlol',
    },
    :remote_path => '/home/deploy/puff-puff-dash',
    :remote_user => 'deploy',
    :ssh_port    => 2222,
    :host        => 'ppd.intern.xyzyxyzy.xyz'
  }
}

$env = (ENV['ENV'] || 'dev').to_sym
fail "Invalid environment #{$env}" unless [:dev, :prod].include?($env)

def connect_db(db_config)
  begin
    PG.connect user:     db_config[:username], \
               password: db_config[:password], \
               dbname:   db_config[:database], \
               port:     db_config[:host_port], \
               host:     db_config[:host]
  rescue Exception => e
    puts e if ENV['DEBUG']
  end
end

def container_running?(name)
  containers = `docker ps | tail -n+2 | awk '{ print $NF }'`.split(/\n/)
  containers.include? name
end

def container_exists?(name)
  containers = `docker ps -a | tail -n+2 | awk '{ print $NF }'`.split(/\n/)
  containers.include? name
end

namespace :dev do
  namespace :db do
    @db = connect_db $config[:db]

    def existing_migrations
      Dir.glob('resources/migrations/*.up.sql').map do |f|
        matches = /^(\d+)-([a-z-]+)\.up\.sql$/.match File.basename(f)
        if matches
          {
            :id   => matches[1],
            :name => matches[2].gsub(/-/, ' '),
            :file => f
          }
        end
      end
    end

    def pending_migrations
      run = @db.query('select id from schema_migrations')
      run_ids = run.map { |r| r['id'] }.sort
      existing_migrations.reject { |e| run_ids.include? e[:id] }
    end

    desc 'Start the database container'
    task :start do
      container_name = $config[:db][:container_name]
      password = $config[:db][:password]
      image = $config[:db][:image]
      port = $config[:db][:host_port]

      case
      when container_running?(container_name)
        puts "Container '#{container_name}' already running"
      when container_exists?(container_name)
        sh "docker start #{container_name}"
      else
        cmd = <<-EOT
docker run \
  --name #{container_name} -e POSTGRES_PASSWORD=#{password} -d -p 127.0.0.1:#{port}:5432 #{image}
EOT
        sh cmd
      end
    end

    desc 'Stop the database container'
    task :stop do
      container_name = $config[:db][:container_name]
      if container_running?(container_name)
        sh "docker stop #{container_name}"
      end
    end

    desc 'Run a psql shell'
    task :shell do
      container_name = $config[:db][:container_name]
      password = $config[:db][:password]
      username = $config[:db][:username]
      image = $config[:db][:image]

      fail 'Database container not running' unless container_running?(container_name)

      cmd = <<-EOT
docker run \
  -e PGPASSWORD=#{password} \
  -it --rm --link #{container_name}:#{image} #{image} psql -h postgres -U #{username}
EOT
      sh cmd
    end

    desc 'Show pending migrations'
    task :show_pending do
      fail 'No database connection' if @db.nil?
      pending_migrations.each do |m|
        printf "%-16s%s\n" % [m[:id], m[:name]]
      end
    end

    desc 'Run pending migrations'
    task :run_migrations do
      fail 'No database connection' if @db.nil?
      if not pending_migrations.empty?
        sh 'lein migratus'
      else
        puts 'No migrations to run'
      end
    end

    desc 'Create a new migration'
    task :new_migration, [:name] do |_, args|
      fail 'Missing required name' if not args[:name]
      sh "lein migratus create #{args[:name]}"
      new_file = pending_migrations.sort_by { |m| m[:id] }.first[:file]
      puts "Created #{new_file}"
    end
  end

  desc 'Start services'
  task :start do
    commands = $config[:services].values
    tmux_commands = commands[0..-2].map { 'tmux split' }
    tmux_commands << commands.map.with_index do |c, i|
      "tmux send-keys -t #{(i + 1)} '#{c}' Enter"
    end

    start_command = tmux_commands.join ' && '
    sh start_command
  end
end

namespace :prod do
  desc 'Deploy to remote'
  task :deploy do
    port = $config[:deploy][:ssh_port]
    user = $config[:deploy][:remote_user]
    host = $config[:deploy][:host]
    path = $config[:deploy][:remote_path]
    ssh_options = { :port => port, :verbose => :error }

    remote_commands = [
      'docker-compose down',
      'docker-compose build',
      'docker-compose up -d'
    ].map { |c| "cd #{path} && #{c}" }

    # TODO move to deploy config
    files = [
      'target/uberjar/*.jar',
      'docker-compose.yml',
      'Dockerfile',
      'Dockerfile-db',
      'Rakefile',
      'resources/migrations/',
      'resources/bin/',
    ].join(' ')

    sync_cmd = "rsync -raRve 'ssh -p#{port}' --exclude='.git/' #{files} #{user}@#{host}:#{path}"

    sh 'lein uberjar' unless (ENV['NO_BUILD'] || ENV['NO_SYNC'])
    sh sync_cmd unless ENV['NO_SYNC']
    Net::SSH.start(host, user, ssh_options) do |ssh|
      remote_commands.each { |c| puts ssh.exec!(c) }
      ssh.loop
    end
  end

  namespace :db do
    desc 'Run a prod psql shell'
    task :shell do
      container_name = $config[:deploy][:db][:container_name]
      password = $config[:deploy][:db][:password]
      username = $config[:deploy][:db][:username]

      fail 'Database container not running' unless container_running?(container_name)

      cmd = <<-EOT
PGPASSWORD=#{password} \
docker exec \
  -it #{container_name} psql -h localhost -U #{username}
EOT
      sh cmd
    end
  end
end

desc 'Search posts by field with LIKE match'
task :search, [:field, :term, :show] do |_, args|
  field = args[:field] or fail 'No field provided'
  term = args[:term] or fail 'No term provided'
  show = args[:show] or nil

  host = case $env
  when :dev
    'localhost:3000'
  when :prod
    $config[:deploy][:host]
  end

  query = {
    :query => {
      :where => [
        {
          :cmp => :like,
          :field => field,
          :value => term
        }
      ]
    }
  }

  cmd = <<-EOT.strip.gsub(/  */, ' ')
    curl -XPOST -H 'Content-type: application/json' \
      -d '#{query.to_json}' #{host}/links
  EOT

  if system('which jq >/dev/null')
    jq_cmd = '.links[]'

    if show
      jq_return = show.split(/:/).map do |f|
        "#{f.split(/\./).last}: .#{f}"
      end.join(', ')

      jq_cmd += " | { #{jq_return} }"
    end

    cmd += " | jq '#{jq_cmd}'"
  end

  sh cmd
end

desc 'Import links from a file'
task :import, [:file, :source, :tag] do |t, args|
  file = args[:file] or fail 'Missing file'
  source = args[:source] or fail 'Missing source'
  tag = args[:tag] or nil

  host = case $env
         when :dev
           'localhost:3000'
         when :prod
           $config[:deploy][:host]
         end
  url = "http://#{host}/links/#{source}"
  if not tag.nil?
    url += "?tag=#{tag}"
  end

  # TODO cmd_for command + hash of option keys and vals
  cmd = <<-EOT
curl \
  -XPOST \
  -H 'Content-Type: application/json' \
  --data @#{file} \
  #{url}
EOT
  sh cmd
end
