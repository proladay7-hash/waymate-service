// Seed script: Populates realistic, rich data into Neo4j via waymate-service REST APIs for all features
const BASE_URL = 'http://localhost:8781';

async function postJson(endpoint, data) {
  try {
    const res = await fetch(`${BASE_URL}${endpoint}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data),
    });
    const text = await res.text();
    try {
      return JSON.parse(text);
    } catch {
      return { raw: text, status: res.status };
    }
  } catch (err) {
    console.error(`Error POST ${endpoint}:`, err.message);
    return null;
  }
}

async function getJson(endpoint) {
  try {
    const res = await fetch(`${BASE_URL}${endpoint}`);
    return await res.json();
  } catch (err) {
    console.error(`Error GET ${endpoint}:`, err.message);
    return null;
  }
}

async function seed() {
  console.log('🚀 Starting WayMate Comprehensive Database Seeder via REST APIs...\n');

  // 1. Get or Create Main User (Lam Le)
  console.log('--- 1. Authenticating / Retrieving Main User ---');
  let lamUser = null;
  const loginRes = await postJson('/api/v1/user/signin', {
    email: 'proladay7@gmail.com',
    password: '023345458',
  });
  if (loginRes && loginRes.user) {
    lamUser = loginRes.user;
    console.log(`✅ Main User found: ${lamUser.name} (${lamUser.id})`);
  } else {
    const signupRes = await postJson('/api/v1/user/signup', {
      name: 'Lam Le',
      email: 'proladay7@gmail.com',
      password: '023345458',
      avatar: 'https://images.unsplash.com/photo-1570295999919-56ceb5ecca61?auto=format&fit=crop&w=250&q=80',
      bio: 'Adventure Tourer & Motorbike Enthusiast | BMW S1000RR & Yamaha MT-03',
    });
    lamUser = signupRes?.user;
    console.log(`✅ Main User registered: ${lamUser?.name} (${lamUser?.id})`);
  }

  // 2. Register Community Riders
  console.log('\n--- 2. Registering Community Riders ---');
  const communityRiders = [
    {
      name: 'Alex "Viper" Mercer',
      email: 'alex@waymate.com',
      password: '023345458',
      avatar: 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=250&q=80',
      bio: 'Honda CB500X Rider | 50,000+ km across Vietnam & Indochina loops',
    },
    {
      name: 'Sarah "Blade" Connor',
      email: 'sarah@waymate.com',
      password: '023345458',
      avatar: 'https://images.unsplash.com/photo-1544005313-94ddf0286df2?auto=format&fit=crop&w=250&q=80',
      bio: 'Ducati Monster 821 | Highland passes & weekend canyon carver',
    },
    {
      name: 'Minh Việt ADV',
      email: 'viet@waymate.com',
      password: '023345458',
      avatar: 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&w=250&q=80',
      bio: 'BMW R1250GS Adventure | Off-road scout & overland expedition leader',
    },
  ];

  const riderMap = {};
  riderMap['Lam Le'] = lamUser;

  for (const r of communityRiders) {
    let u = null;
    const signin = await postJson('/api/v1/user/signin', { email: r.email, password: r.password });
    if (signin?.user) {
      u = signin.user;
    } else {
      const sup = await postJson('/api/v1/user/signup', r);
      u = sup?.user;
    }
    if (u) {
      riderMap[r.name] = u;
      console.log(`✅ Rider ready: ${u.name} (${u.id})`);
    }
  }

  // 3. Register Vehicles & Maintenance History for Lam Le
  console.log('\n--- 3. Seeding Garage Vehicles & Maintenance in Neo4j ---');
  if (lamUser?.id) {
    // Check existing vehicles
    const existingVehs = await getJson(`/api/v1/vehicle?userId=${lamUser.id}`);
    const vehNames = Array.isArray(existingVehs) ? existingVehs.map((v) => v.name) : [];

    // Add BMW S1000RR if not exists
    if (!vehNames.includes('S1000RR M-Package')) {
      const v = await postJson(`/api/v1/vehicle?userId=${lamUser.id}`, {
        name: 'S1000RR M-Package',
        brand: 'BMW Motorrad',
        plateNumber: '59-A1 999.88',
        odo: 14200,
        date: 2023,
        capacity: 1000,
      });
      if (v?.id) {
        console.log(`✅ Added BMW S1000RR (${v.id})`);
        await postJson(`/api/v1/maintenance?vehicleId=${v.id}`, {
          name: 'Motul 300V 10W40 Factory Line & K&N Oil Filter',
          total: 1850000,
          location: 'BMW Motorrad Official Workshop',
          maintenanceRequests: [
            { name: 'Motul 300V 10W40 (4L)', price: 1600000 },
            { name: 'K&N Performance Oil Filter', price: 250000 },
          ],
        });
        await postJson(`/api/v1/maintenance?vehicleId=${v.id}`, {
          name: 'DID 525 ZVMX Gold Chain Service & Lube',
          total: 350000,
          location: 'WayMate Garage Hub',
          maintenanceRequests: [
            { name: 'Chain Cleaning & Tension Adjustment', price: 150000 },
            { name: 'Motul Chain Lube Factory Line', price: 200000 },
          ],
        });
      }
    }

    // Add Ford Ranger Wildtrak if not exists
    if (!vehNames.includes('Ranger Wildtrak 4x4')) {
      const v = await postJson(`/api/v1/vehicle?userId=${lamUser.id}`, {
        name: 'Ranger Wildtrak 4x4',
        brand: 'Ford',
        plateNumber: '51D-888.99',
        odo: 36500,
        date: 2022,
        capacity: 2000,
      });
      if (v?.id) {
        console.log(`✅ Added Ford Ranger Wildtrak (${v.id})`);
        await postJson(`/api/v1/maintenance?vehicleId=${v.id}`, {
          name: 'Castrol Magnatec 5W-30 & Air Filter Replacement',
          total: 2450000,
          location: 'Ford Saigon Centre',
          maintenanceRequests: [
            { name: 'Engine Oil Castrol Magnatec 6L', price: 1800000 },
            { name: 'OEM Air & Fuel Filters', price: 650000 },
          ],
        });
      }
    }
  }

  // 4. Seed Adventure Trips in Neo4j
  console.log('\n--- 4. Seeding Adventure Trips in Neo4j ---');
  const existingTrips = await getJson('/api/v1/trips');
  if (!Array.isArray(existingTrips) || existingTrips.length === 0) {
    const tripsToSeed = [
      {
        id: 'trip-1',
        title: 'Ha Giang Extreme Northern Loop',
        titleVie: 'Cung đường phượt đỉnh cao Vòng cung Hà Giang',
        riderName: 'Minh Việt ADV',
        riderAvatar: 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&w=150&q=80',
        riderBadge: 'Ha Giang Master',
        riderBadgeVie: 'Bậc thầy Hà Giang',
        bikeModel: 'Adventure Tourer 1200',
        distanceKm: 350,
        avgVelocityKmh: 48,
        maxVelocityKmh: 82,
        groupSize: '4 / 6 Riders',
        duration: '3 Days, 2 Nights',
        durationVie: '3 Ngày, 2 Đêm',
        dates: 'Nov 12 - Nov 14, 2026',
        category: 'popular',
        coverImage: 'https://images.unsplash.com/photo-1568772585407-9361f9bf3a87?auto=format&fit=crop&w=800&q=80',
        likesCount: 142,
        commentsCount: 28,
        totalCostVnd: '4,200,000 VND',
        totalCostUsd: '$170 USD',
        description: 'Epic mountain pass ride conquering Ma Pi Leng Pass, Tu San Canyon, and the dramatic winding limestone passes of Northern Vietnam.',
        descriptionVie: 'Hành trình chinh phục Đèo Mã Pí Lèng hùng vĩ, hẻm Tu Sản và những cung đèo ngoạn mục tại địa đầu Tổ quốc.',
        googleMapsUrl: 'https://maps.app.goo.gl/MaPiLengPass',
        destinationJson: JSON.stringify({
          name: 'Ma Pi Leng Pass, Ha Giang',
          nameVie: 'Đèo Mã Pí Lèng, Hà Giang',
          latitude: 23.2386,
          longitude: 105.4194,
          address: 'Đèo Mã Pí Lèng, Pải Lủng, Mèo Vạc, Hà Giang, Vietnam',
          mapUrl: 'https://maps.app.goo.gl/MaPiLengPass',
        }),
        eatDrinkJson: JSON.stringify([
          { name: 'Phở Cụ Thành - Đồng Văn', type: 'Local Breakfast', rating: '4.9 ⭐', cost: '50,000 VND' },
          { name: 'Mã Pí Lèng Sunset Coffee', type: 'Viewpoint Rest', rating: '4.8 ⭐', cost: '40,000 VND' },
        ]),
        landmarksJson: JSON.stringify([
          { name: 'Mã Pí Lèng Skywalk', stopTime: '1.5 hrs', fee: 'Free' },
          { name: 'Cột cờ Lũng Cú', stopTime: '2 hrs', fee: '20,000 VND' },
        ]),
        lodgingJson: JSON.stringify([
          { name: 'Bụi Homestay Đồng Văn', price: '250,000 VND/night', status: 'Verified' },
          { name: 'Mã Pí Lèng Ecolodge', price: '750,000 VND/night', status: 'Scenic View' },
        ]),
      },
      {
        id: 'trip-2',
        title: 'Sai Gon to Da Lat - The Highland Rush',
        titleVie: 'Sài Gòn - Đà Lạt: Cung đèo sương mù Cao nguyên',
        riderName: 'Lam Le',
        riderAvatar: 'https://images.unsplash.com/photo-1570295999919-56ceb5ecca61?auto=format&fit=crop&w=250&q=80',
        riderBadge: 'Highland Legend',
        riderBadgeVie: 'Chiến thần Cao nguyên',
        bikeModel: 'BMW S1000RR M-Package',
        distanceKm: 310,
        avgVelocityKmh: 62,
        maxVelocityKmh: 115,
        groupSize: '6 / 8 Riders',
        duration: '2 Days, 1 Night',
        durationVie: '2 Ngày, 1 Đêm',
        dates: 'Every Weekend',
        category: 'popular',
        coverImage: 'https://images.unsplash.com/photo-1558981403-c5f9899a28bc?auto=format&fit=crop&w=800&q=80',
        likesCount: 230,
        commentsCount: 45,
        totalCostVnd: '2,800,000 VND',
        totalCostUsd: '$115 USD',
        description: 'Thrilling mountain curves via Bao Loc Pass and Prenn Pass with cool pine forest hairpins.',
        descriptionVie: 'Thử thách ôm cua Đèo Bảo Lộc và Đèo Prenn, ngắm bình minh trên đồi thông thơ mộng.',
        googleMapsUrl: 'https://maps.app.goo.gl/PrennPassDaLat',
        destinationJson: JSON.stringify({
          name: 'Prenn Pass, Da Lat',
          nameVie: 'Đèo Prenn, Đà Lạt',
          latitude: 11.9056,
          longitude: 108.4412,
          address: 'Đèo Prenn, Phường 3, TP. Đà Lạt, Lâm Đồng, Vietnam',
          mapUrl: 'https://maps.app.goo.gl/PrennPassDaLat',
        }),
        eatDrinkJson: JSON.stringify([
          { name: 'Cơm Niêu Thuận Kiều Bảo Lộc', type: 'Rest Stop Lunch', rating: '4.7 ⭐', cost: '120,000 VND' },
          { name: 'Quán Cafe Đỉnh Đèo Gió', type: 'Panoramic View', rating: '4.8 ⭐', cost: '45,000 VND' },
        ]),
        landmarksJson: JSON.stringify([
          { name: 'Đèo Bảo Lộc Hairpin Turns', stopTime: '45 mins', fee: 'Free' },
          { name: 'Hồ Tuyền Lâm Sunset Point', stopTime: '1 hr', fee: 'Free' },
        ]),
        lodgingJson: JSON.stringify([
          { name: 'The Wilder-nest Da Lat', price: '600,000 VND/night', status: 'Pine Forest' },
        ]),
      },
      {
        id: 'trip-3',
        title: 'National Highway 1 & Hai Van Pass Coastal Odyssey',
        titleVie: 'Thiên hạ đệ nhất hùng quan - Đèo Hải Vân & Duyên hải miền Trung',
        riderName: 'Alex "Viper" Mercer',
        riderAvatar: 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=250&q=80',
        riderBadge: 'Coast Conqueror',
        riderBadgeVie: 'Bậc thầy đường ven biển',
        bikeModel: 'Honda CB500X',
        distanceKm: 420,
        avgVelocityKmh: 68,
        maxVelocityKmh: 105,
        groupSize: '8 / 10 Riders',
        duration: '4 Days, 3 Nights',
        durationVie: '4 Ngày, 3 Đêm',
        dates: 'Dec 05 - Dec 08, 2026',
        category: 'nearby',
        coverImage: 'https://images.unsplash.com/photo-1508214751196-bcfd4ca60f91?auto=format&fit=crop&w=800&q=80',
        likesCount: 185,
        commentsCount: 39,
        totalCostVnd: '5,500,000 VND',
        totalCostUsd: '$220 USD',
        description: 'Coastal roads winding between steep mountains and the Pacific Ocean. Top Gear voted this route one of the best coastal drives.',
        descriptionVie: 'Cung đường ven biển đẹp bậc nhất thế giới, ôm trọn vịnh Lăng Cô và bán đảo Sơn Trà tuyệt đẹp.',
        googleMapsUrl: 'https://maps.app.goo.gl/HaiVanPass',
        destinationJson: JSON.stringify({
          name: 'Hai Van Pass Summit',
          nameVie: 'Đỉnh Đèo Hải Vân',
          latitude: 16.1953,
          longitude: 108.1322,
          address: 'Hải Vân Quan, TT. Lăng Cô, Phú Lộc, Thừa Thiên Huế, Vietnam',
          mapUrl: 'https://maps.app.goo.gl/HaiVanPass',
        }),
        eatDrinkJson: JSON.stringify([
          { name: 'Hải Sản Đầm Lập An Lăng Cô', type: 'Seafood Lunch', rating: '4.8 ⭐', cost: '250,000 VND' },
          { name: 'Cafe Đỉnh Hải Vân Quan', type: 'Cloud Viewpoint', rating: '4.9 ⭐', cost: '35,000 VND' },
        ]),
        landmarksJson: JSON.stringify([
          { name: 'Di tích Hải Vân Quan', stopTime: '1 hr', fee: 'Free' },
          { name: 'Cây Thông Cô Đơn', stopTime: '30 mins', fee: 'Free' },
        ]),
        lodgingJson: JSON.stringify([
          { name: 'Lang Co Beach Resort', price: '950,000 VND/night', status: 'Beachfront' },
        ]),
      },
    ];

    for (const t of tripsToSeed) {
      const res = await postJson('/api/v1/trips', t);
      console.log(`✅ Seeded Trip: ${res?.title || t.title}`);
    }
  } else {
    console.log(`ℹ️ Trips already exist (${existingTrips.length} trips)`);
  }

  // 5. Seed Community Social Posts in Neo4j
  console.log('\n--- 5. Seeding Community Social Posts in Neo4j ---');
  const existingPosts = await getJson('/api/v1/posts');
  if (!Array.isArray(existingPosts) || existingPosts.length === 0) {
    const alex = riderMap['Alex "Viper" Mercer'];
    const sarah = riderMap['Sarah "Blade" Connor'];
    const viet = riderMap['Minh Việt ADV'];

    if (alex?.id) {
      const p1 = await postJson('/api/v1/posts', {
        authorId: alex.id,
        authorName: alex.name,
        authorAvatar: alex.avatar,
        content: 'Chinh phục Đèo Hải Vân buổi bình minh cùng hội anh em CBR650R & Monster 821! Không khí buổi sáng mát lạnh, ôm cua cực sướng anh em ơi! 🏍️💨',
        mediaUrls: [
          'https://images.unsplash.com/photo-1558981403-c5f9899a28bc?auto=format&fit=crop&w=800&q=80',
          'https://images.unsplash.com/photo-1568772585407-9361f9bf3a87?auto=format&fit=crop&w=800&q=80',
        ],
        tripId: 'trip-3',
        locationName: 'Đèo Hải Vân, Đà Nẵng',
      });
      console.log(`✅ Seeded Post by Alex (${p1?.id})`);

      if (p1?.id && sarah?.id) {
        await postJson(`/api/v1/posts/${p1.id}/comments`, {
          authorId: sarah.id,
          authorName: sarah.name,
          authorAvatar: sarah.avatar,
          content: 'Cung này chạy sáng sớm ngắm bình minh view biển là hết nước chấm! Kèo sau cho em đu theo với! 🔥',
        });
        await postJson(`/api/v1/posts/${p1.id}/like`, { userId: sarah.id });
      }
      if (p1?.id && lamUser?.id) {
        await postJson(`/api/v1/posts/${p1.id}/comments`, {
          authorId: lamUser.id,
          authorName: lamUser.name,
          authorAvatar: lamUser.avatar,
          content: 'Đoạn cua tay áo gần đỉnh đèo mặt đường có còn dăm không Alex ơi? Cuối tuần này mình cũng dẫn đoàn qua.',
        });
        await postJson(`/api/v1/posts/${p1.id}/like`, { userId: lamUser.id });
      }
    }

    if (sarah?.id) {
      const p2 = await postJson('/api/v1/posts', {
        authorId: sarah.id,
        authorName: sarah.name,
        authorAvatar: sarah.avatar,
        content: 'Check-in Mã Pí Lèng - Con đèo huyền thoại của Tây Bắc. Cung đường uốn lượn bên hẻm Tu Sản hùng vĩ. Một chuyến đi xứng đáng 10/10 cho thanh xuân! 🏔️✨',
        mediaUrls: [
          'https://images.unsplash.com/photo-1508214751196-bcfd4ca60f91?auto=format&fit=crop&w=800&q=80',
        ],
        tripId: 'trip-1',
        locationName: 'Mã Pí Lèng, Mèo Vạc, Hà Giang',
      });
      console.log(`✅ Seeded Post by Sarah (${p2?.id})`);

      if (p2?.id && viet?.id) {
        await postJson(`/api/v1/posts/${p2.id}/comments`, {
          authorId: viet.id,
          authorName: viet.name,
          authorAvatar: viet.avatar,
          content: 'Đoạn từ Đồng Văn sang Mèo Vạc đi mùa này lúa chín và tam giác mạch nở đẹp lắm em gái ơi!',
        });
        await postJson(`/api/v1/posts/${p2.id}/like`, { userId: viet.id });
      }
    }
  } else {
    console.log(`ℹ️ Posts already exist (${existingPosts.length} posts)`);
  }

  // 6. Seed Chat Rooms & Real-time Messages in Neo4j
  console.log('\n--- 6. Seeding Chat Rooms & Messages in Neo4j ---');
  if (lamUser?.id) {
    const alex = riderMap['Alex "Viper" Mercer'];
    const sarah = riderMap['Sarah "Blade" Connor'];

    // Direct room with Sarah
    if (sarah?.id) {
      const direct = await postJson('/api/v1/chat/rooms/direct', {
        currentUserId: lamUser.id,
        otherUserId: sarah.id,
      });
      if (direct?.id) {
        console.log(`✅ Direct room created: Lam Le <-> Sarah Connor (${direct.id})`);
        await postJson(`/api/v1/chat/rooms/${direct.id}/messages`, {
          roomId: direct.id,
          senderId: sarah.id,
          senderName: sarah.name,
          senderAvatar: sarah.avatar,
          content: 'Hi Lâm! Cuối tuần này hội mình có chạy tour Đà Lạt không anh? Em vừa thay nhớt Motul 300V cho em Monster 821 xong!',
          type: 'TEXT',
        });
        await postJson(`/api/v1/chat/rooms/${direct.id}/messages`, {
          roomId: direct.id,
          senderId: lamUser.id,
          senderName: lamUser.name,
          senderAvatar: lamUser.avatar,
          content: 'Chào Sarah! Sáng thứ 7 này 5h30 xuất phát ở trạm thu phí Long Phước nhé em. Đi cung QL20 qua Đèo Bảo Lộc ngắm mây!',
          type: 'TEXT',
        });
      }
    }

    // Group room: Sài Gòn - Đà Lạt Weekend Riders
    if (alex?.id && sarah?.id) {
      const grp = await postJson('/api/v1/chat/rooms/group', {
        creatorId: lamUser.id,
        name: '🏍️ Sài Gòn - Đà Lạt Weekend Riders',
        avatar: 'https://images.unsplash.com/photo-1558981403-c5f9899a28bc?auto=format&fit=crop&w=300&q=80',
        memberIds: [lamUser.id, alex.id, sarah.id],
      });
      if (grp?.id) {
        console.log(`✅ Group room created: ${grp.name} (${grp.id})`);
        await postJson(`/api/v1/chat/rooms/${grp.id}/messages`, {
          roomId: grp.id,
          senderId: alex.id,
          senderName: alex.name,
          senderAvatar: alex.avatar,
          content: 'Chào cả nhà! Anh em kiểm tra kĩ bố thắng và áp suất lốp trước khi xuất phát nhé, đoạn đèo Bảo Lộc đang sửa vài khúc cua.',
          type: 'TEXT',
        });
        await postJson(`/api/v1/chat/rooms/${grp.id}/messages`, {
          roomId: grp.id,
          senderId: alex.id,
          senderName: alex.name,
          senderAvatar: alex.avatar,
          content: '📍 Tọa độ điểm tập kết cà phê sáng ngắm bình minh trên đỉnh đèo:',
          type: 'LOCATION',
          metadataJson: JSON.stringify({
            lat: 11.4582,
            lng: 107.7214,
            name: 'Đỉnh Đèo Bảo Lộc, Lâm Đồng',
            address: 'Đèo Bảo Lộc, Quốc lộ 20, Lâm Đồng',
          }),
        });
      }
    }
  }

  // 7. Seed Emergency SOS Requests in Neo4j
  console.log('\n--- 7. Seeding Emergency SOS Requests in Neo4j ---');
  const alex = riderMap['Alex "Viper" Mercer'];
  if (alex?.id) {
    const sos = await postJson('/api/v1/sos', {
      userId: alex.id,
      name: 'Flat Tyre / Thủng lốp',
      location: 'Đèo Prenn, TP. Đà Lạt, Lâm Đồng',
      latitude: 11.9056,
      longitude: 108.4412,
      description: 'Bị cán đinh xì bánh sau ở khúc cua số 4. Cần anh em nào có bơm mini hoặc bộ vá lốp không săm hỗ trợ giúp!',
      phoneNumber: '0901234567',
      bikeInfo: 'Honda CB500X • 59-X1 999.88',
    });
    console.log(`✅ Seeded Emergency SOS Alert on Pass Prenn: [id=${sos?.id}]`);
  }

  console.log('\n🎉 ALL FEATURES SEEDED IN NEO4J VIA APIS SUCCESSFULLY!');
}

seed().catch(console.error);
