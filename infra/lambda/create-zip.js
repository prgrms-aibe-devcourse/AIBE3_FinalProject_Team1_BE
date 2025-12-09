const fs = require('fs');
const archiver = require('archiver');
const path = require('path');

console.log('==========================================');
console.log('Lambda 함수 패키지 생성 (Node.js)');
console.log('==========================================');

const output = fs.createWriteStream('profile_resizer.zip');
const archive = archiver('zip', {
    zlib: { level: 9 } // 최대 압축
});

output.on('close', function() {
    console.log('');
    console.log('==========================================');
    console.log('✅ Lambda 패키지 생성 완료!');
    console.log('==========================================');
    console.log('');
    console.log(`파일 크기: ${(archive.pointer() / 1024 / 1024).toFixed(2)} MB`);
    console.log('파일 위치: profile_resizer.zip');
    console.log('');
    console.log('==========================================');
    console.log('다음 단계:');
    console.log('1. cd ..');
    console.log('2. terraform plan');
    console.log('3. terraform apply');
    console.log('==========================================');
});

archive.on('error', function(err) {
    throw err;
});

archive.pipe(output);

console.log('');
console.log('📦 파일 압축 중...');

// index.js 추가
archive.file('index.js', { name: 'index.js' });

// node_modules 폴더 추가
archive.directory('node_modules/', 'node_modules');

archive.finalize();